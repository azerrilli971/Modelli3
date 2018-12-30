package com.iota.iri.storage.rocksDB;

import com.iota.iri.model.*;
import com.iota.iri.model.persistables.Address;
import com.iota.iri.model.persistables.Approvee;
import com.iota.iri.model.persistables.Bundle;
import com.iota.iri.model.persistables.Milestone;
import com.iota.iri.model.persistables.ObsoleteTag;
import com.iota.iri.model.persistables.Tag;
import com.iota.iri.model.persistables.Transaction;
import com.iota.iri.storage.Indexable;
import com.iota.iri.storage.Persistable;
import com.iota.iri.storage.PersistenceProvider;
import com.iota.iri.utils.IotaIOUtils;
import com.iota.iri.utils.Pair;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.SystemUtils;
import org.rocksdb.*;
import org.rocksdb.util.SizeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

public class RocksDBPersistenceProvider implements PersistenceProvider {

    private static final Logger log = LoggerFactory.getLogger(RocksDBPersistenceProvider.class);
    private static final int BLOOM_FILTER_BITS_PER_KEY = 10;

    private static final Pair<Indexable, Persistable> PAIR_OF_NULLS = new Pair<>(null, null);

    private final List<String> columnFamilyNames = Arrays.asList(
            new String(RocksDB.DEFAULT_COLUMN_FAMILY),
            "transaction",
            "transaction-metadata",
            "milestone",
            "stateDiff",
            "address",
            "approvee",
            "bundle",
            "obsoleteTag",
            "tag"
    );

    private final List<ColumnFamilyHandle> columnFamilyHandles = new ArrayList<>();
    private final SecureRandom seed = new SecureRandom();

    private final String dbPath;
    private final String logPath;
    private final int cacheSize;

    private ColumnFamilyHandle transactionHandle;
    private ColumnFamilyHandle transactionMetadataHandle;
    private ColumnFamilyHandle milestoneHandle;
    private ColumnFamilyHandle stateDiffHandle;
    private ColumnFamilyHandle addressHandle;
    private ColumnFamilyHandle approveeHandle;
    private ColumnFamilyHandle bundleHandle;
    private ColumnFamilyHandle obsoleteTagHandle;
    private ColumnFamilyHandle tagHandle;

    private Map<Class<?>, ColumnFamilyHandle> classTreeMap;
    private Map<Class<?>, ColumnFamilyHandle> metadataReference;

    private RocksDB db;
    // DBOptions is only used in initDB(). However, it is closeable - so we keep a reference for shutdown.
    private DBOptions options = new DBOptions();
    private BloomFilter bloomFilter;
    private boolean available;

    public RocksDBPersistenceProvider(String dbPath, String logPath, int cacheSize) {
        this.dbPath = dbPath;
        this.logPath = logPath;
        this.cacheSize = cacheSize;
    }

    public class DbException extends Exception {
        public DbException(String msg) {
            super(msg);
        }
    }

    @Override
    public void init() {
        log.info("Initializing Database Backend... ");
        initDB(dbPath, logPath);
        initClassTreeMap();
        available = true;
        log.info("RocksDB persistence provider initialized.");
    }

    @Override
    public boolean isAvailable() {
        return this.available;
    }

    private void initClassTreeMap() {
        Map<Class<?>, ColumnFamilyHandle> classMap = new LinkedHashMap<>();
        classMap.put(Transaction.class, transactionHandle);
        classMap.put(Milestone.class, milestoneHandle);
        classMap.put(StateDiff.class, stateDiffHandle);
        classMap.put(Address.class, addressHandle);
        classMap.put(Approvee.class, approveeHandle);
        classMap.put(Bundle.class, bundleHandle);
        classMap.put(ObsoleteTag.class, obsoleteTagHandle);
        classMap.put(Tag.class, tagHandle);
        classTreeMap = classMap;

        Map<Class<?>, ColumnFamilyHandle> metadataHashMap = new HashMap<>();
        metadataHashMap.put(Transaction.class, transactionMetadataHandle);
        metadataReference = metadataHashMap;
    }

    @Override
    public void shutdown() {
        for (final ColumnFamilyHandle columnFamilyHandle : columnFamilyHandles) {
            IotaIOUtils.closeQuietly(columnFamilyHandle);
        }
        IotaIOUtils.closeQuietly(db, options, bloomFilter);
    }

    @Override
    public boolean save(Persistable thing, Indexable index) throws Exception {
        ColumnFamilyHandle handle = classTreeMap.get(thing.getClass());
        db.put(handle, index.bytes(), thing.bytes());

        ColumnFamilyHandle referenceHandle = metadataReference.get(thing.getClass());
        if (referenceHandle != null) {
            db.put(referenceHandle, index.bytes(), thing.metadata());
        }
        return true;
    }

    @Override
    public void delete(Class<?> model, Indexable index) throws Exception {
        db.delete(classTreeMap.get(model), index.bytes());
    }

    @Override
    public boolean exists(Class<?> model, Indexable key) throws Exception {
        ColumnFamilyHandle handle = classTreeMap.get(model);
        return handle != null && db.get(handle, key.bytes()) != null;
    }

    @Override
    public Set<Indexable> keysWithMissingReferences(Class<?> model, Class<?> other) throws Exception {
        ColumnFamilyHandle handle = classTreeMap.get(model);
        ColumnFamilyHandle otherHandle = classTreeMap.get(other);

        try (RocksIterator iterator = db.newIterator(handle)) {
            Set<Indexable> indexables = null;

            for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                if (db.get(otherHandle, iterator.key()) == null) {
                    indexables = indexables == null ? new HashSet<>() : indexables;
                    indexables.add(HashFactory.GENERIC.create(model, iterator.key()));
                }
            }
            return indexables == null ? Collections.emptySet() : Collections.unmodifiableSet(indexables);
        }
    }

    @Override
    public Persistable get(Class<?> model, Indexable index) throws Exception {
        Persistable object = (Persistable) model.newInstance();
        object.read(db.get(classTreeMap.get(model), index == null ? new byte[0] : index.bytes()));

        ColumnFamilyHandle referenceHandle = metadataReference.get(model);
        if (referenceHandle != null) {
            object.readMetadata(db.get(referenceHandle, index == null ? new byte[0] : index.bytes()));
        }
        return object;
    }

    @Override
    public boolean mayExist(Class<?> model, Indexable index) {
        ColumnFamilyHandle handle = classTreeMap.get(model);
        return db.keyMayExist(handle, index.bytes(), new StringBuilder());
    }

    @Override
    public long count(Class<?> model) throws Exception {
        return getCountEstimate(model);
    }

    private long getCountEstimate(Class<?> model) throws RocksDBException {
        ColumnFamilyHandle handle = classTreeMap.get(model);
        return db.getLongProperty(handle, "rocksdb.estimate-num-keys");
    }

    @Override
    public Set<Indexable> keysStartingWith(Class<?> modelClass, byte[] value) {
        Objects.requireNonNull(value, "value byte[] cannot be null");
        ColumnFamilyHandle handle = classTreeMap.get(modelClass);
        Set<Indexable> keys = null;
        if (handle != null) {
            try (RocksIterator iterator = db.newIterator(handle)) {
                iterator.seek(HashFactory.GENERIC.create(modelClass, value, 0, value.length).bytes());

                byte[] found;
                while (iterator.isValid() && keyStartsWithValue(value, found = iterator.key())) {
                    keys = keys == null ? new HashSet<>() : keys;
                    keys.add(HashFactory.GENERIC.create(modelClass, found));
                    iterator.next();
                }
            }
        }
        return keys == null ? Collections.emptySet() : Collections.unmodifiableSet(keys);
    }

    /**
     * @param value What we are looking for.
     * @param key   The bytes we are searching in.
     * @return true If the {@code key} starts with the {@code value}.
     */
    private static boolean keyStartsWithValue(byte[] value, byte[] key) {
        if (key == null || key.length < value.length) {
            return false;
        }
        for (int n = 0; n < value.length; n++) {
            if (value[n] != key[n]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Persistable seek(Class<?> model, byte[] key) throws Exception {
        Set<Indexable> hashes = keysStartingWith(model, key);
        if (hashes.isEmpty()) {
            return get(model, null);
        }
        if (hashes.size() == 1) {
            return get(model, (Indexable) hashes.toArray()[0]);
        }
        return get(model, (Indexable) hashes.toArray()[seed.nextInt(hashes.size())]);
    }

    private Pair<Indexable, Persistable> modelAndIndex(Class<?> model, Class<? extends Indexable> index, RocksIterator iterator)
            throws InstantiationException, IllegalAccessException, RocksDBException {

        if (!iterator.isValid()) {
            return PAIR_OF_NULLS;
        }

        Indexable indexable = index.newInstance();
        indexable.read(iterator.key());

        Persistable object = (Persistable) model.newInstance();
        object.read(iterator.value());

        ColumnFamilyHandle referenceHandle = metadataReference.get(model);
        if (referenceHandle != null) {
            object.readMetadata(db.get(referenceHandle, iterator.key()));
        }
        return new Pair<>(indexable, object);
    }

    @Override
    public Pair<Indexable, Persistable> next(Class<?> model, Indexable index) throws Exception {
        try (RocksIterator iterator = db.newIterator(classTreeMap.get(model))) {
            iterator.seek(index.bytes());
            iterator.next();
            return modelAndIndex(model, index.getClass(), iterator);
        }
    }

    @Override
    public Pair<Indexable, Persistable> previous(Class<?> model, Indexable index) throws Exception {
        try (RocksIterator iterator = db.newIterator(classTreeMap.get(model))) {
            iterator.seek(index.bytes());
            iterator.prev();
            return modelAndIndex(model, index.getClass(), iterator);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Pair<Indexable, Persistable> latest(Class<?> model, Class<?> indexModel) throws Exception {
        try (RocksIterator iterator = db.newIterator(classTreeMap.get(model))) {
            iterator.seekToLast();
            return modelAndIndex(model, (Class<Indexable>) indexModel, iterator);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Pair<Indexable, Persistable> first(Class<?> model, Class<?> index) throws Exception {
        try (RocksIterator iterator = db.newIterator(classTreeMap.get(model))) {
            iterator.seekToFirst();
            return modelAndIndex(model, (Class<Indexable>) index, iterator);
        }
    }

    @Override
    public boolean saveBatch(List<Pair<Indexable, Persistable>> models) throws Exception {
        try (WriteBatch writeBatch = new WriteBatch();
             WriteOptions writeOptions = new WriteOptions()) {

            for (Pair<Indexable, Persistable> entry : models) {

                Indexable key = entry.low;
                Persistable value = entry.hi;

                ColumnFamilyHandle handle = classTreeMap.get(value.getClass());
                ColumnFamilyHandle referenceHandle = metadataReference.get(value.getClass());

                if (value.merge()) {
                    writeBatch.merge(handle, key.bytes(), value.bytes());
                } else {
                    writeBatch.put(handle, key.bytes(), value.bytes());
                }
                if (referenceHandle != null) {
                    writeBatch.put(referenceHandle, key.bytes(), value.metadata());
                }
            }

            db.write(writeOptions, writeBatch);
            return true;
        }
    }

    public void deleteBatch(Collection<Pair<Indexable, ? extends Class<? extends Persistable>>> models)
            throws Exception {
        if (CollectionUtils.isNotEmpty(models)) {
            try (WriteBatch writeBatch = new WriteBatch();
             WriteOptions writeOptions = new WriteOptions();) {
                models.forEach(entry -> {
                    Indexable indexable = entry.low;
                    byte[] keyBytes = indexable.bytes();
                    ColumnFamilyHandle handle = classTreeMap.get(entry.hi);
                    writeBatch.remove(handle, keyBytes);
                    ColumnFamilyHandle metadataHandle = metadataReference.get(entry.hi);
                    if (metadataHandle != null) {
                        writeBatch.remove(metadataHandle, keyBytes);
                    }
                });
                        //We are explicit about what happens if the node reboots before a flush to the db
                        writeOptions.setDisableWAL(false)
                        //We want to make sure deleted data was indeed deleted
                        .setSync(true);
                db.write(writeOptions, writeBatch);
            }
        }
    }

    @Override
    public void clear(Class<?> column) throws Exception {
        log.info("Deleting: {} entries", column.getSimpleName());
        flushHandle(classTreeMap.get(column));
    }

    @Override
    public void clearMetadata(Class<?> column) throws Exception {
        log.info("Deleting: {} metadata", column.getSimpleName());
        flushHandle(metadataReference.get(column));
    }

    private void flushHandle(ColumnFamilyHandle handle) throws RocksDBException {
        List<byte[]> itemsToDelete = new ArrayList<>();
        try (RocksIterator iterator = db.newIterator(handle)) {

            for (iterator.seekToLast(); iterator.isValid(); iterator.prev()) {
                itemsToDelete.add(iterator.key());
            }
        }

        if(log.isDebugEnabled() && !itemsToDelete.isEmpty()) {
            log.info(String.format("Amount to delete: %d", itemsToDelete.size()));
        }

        int counter = 0;
        for (byte[] itemToDelete : itemsToDelete) {
            if (++counter % 10000 == 0) {
                log.info("Deleted: {}", counter);
            }
            db.delete(handle, itemToDelete);
        }
    }

    @Override
    public boolean update(Persistable thing, Indexable index, String item) throws Exception {
        ColumnFamilyHandle referenceHandle = metadataReference.get(thing.getClass());
        if (referenceHandle != null) {
            db.put(referenceHandle, index.bytes(), thing.metadata());
        }
        return false;
    }

    // 2018 March 28 - Unused Code
    public void createBackup(String path) throws RocksDBException {
        try (Env env = Env.getDefault();
             BackupableDBOptions backupableDBOptions = new BackupableDBOptions(path);
             BackupEngine backupEngine = BackupEngine.open(env, backupableDBOptions)) {

            backupEngine.createNewBackup(db, true);
        }
    }

    private void initDB(String path, String logPath) {
        try (MergeOperator mergeOperator = new StringAppendOperator();
             ColumnFamilyOptions columnFamilyOptions = new ColumnFamilyOptions();
             ) {
            nestedTry();

            File pathToLogDir = Paths.get(logPath).toFile();
            if (!pathToLogDir.exists() || !pathToLogDir.isDirectory()) {
                boolean success = pathToLogDir.mkdir();
                if (!success) {
                    log.warn("Unable to make directory: {}", pathToLogDir);
                }
            }

            int numThreads = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
            RocksEnv.getDefault()
                    .setBackgroundThreads(numThreads, RocksEnv.FLUSH_POOL)
                    .setBackgroundThreads(numThreads, RocksEnv.COMPACTION_POOL);

            options.setCreateIfMissing(true)
                    .setCreateMissingColumnFamilies(true)
                    .setDbLogDir(logPath)
                    .setMaxLogFileSize(SizeUnit.MB)
                    .setMaxManifestFileSize(SizeUnit.MB)
                    .setMaxOpenFiles(10000)
                    .setMaxBackgroundCompactions(1);

            options.setMaxSubcompactions(Runtime.getRuntime().availableProcessors());

            bloomFilter = new BloomFilter(BLOOM_FILTER_BITS_PER_KEY);

            BlockBasedTableConfig blockBasedTableConfig = new BlockBasedTableConfig().setFilter(bloomFilter);
            blockBasedTableConfig
                    .setFilter(bloomFilter)
                    .setCacheNumShardBits(2)
                    .setBlockSizeDeviation(10)
                    .setBlockRestartInterval(16)
                    .setBlockCacheSize(cacheSize * SizeUnit.KB)
                    .setBlockCacheCompressedNumShardBits(10)
                    .setBlockCacheCompressedSize(32 * SizeUnit.KB);

            columnFamilyOptions.setMergeOperator(mergeOperator)
                    .setTableFormatConfig(blockBasedTableConfig)
                    .setMaxWriteBufferNumber(2)
                    .setWriteBufferSize(2 * SizeUnit.MB);

            options.setAllowConcurrentMemtableWrite(true);

            List<ColumnFamilyDescriptor> columnFamilyDescriptors = new ArrayList<>();
            for (String name : columnFamilyNames) {
                columnFamilyDescriptors.add(new ColumnFamilyDescriptor(name.getBytes(), columnFamilyOptions));
            }

            db = RocksDB.open(options, path, columnFamilyDescriptors, columnFamilyHandles);
            db.enableFileDeletions(true);

            fillModelColumnHandles();

        } catch (Exception e) {
            log.error("Error while initializing RocksDb", e);
            IotaIOUtils.closeQuietly(db);
        }
    }

    private void nestedTry() {
        try {
            RocksDB.loadLibrary();
        } catch (Exception e) {
            if (SystemUtils.IS_OS_WINDOWS) {
                log.error("Error loading RocksDB library. Please ensure that " +
                        "Microsoft Visual C++ 2015 Redistributable Update 3 " +
                        "is installed and updated");
            }
            throw e;
        }
    }

    private void fillModelColumnHandles() throws RocksDBException {
        int i = 0;
        transactionHandle = columnFamilyHandles.get(++i);
        transactionMetadataHandle = columnFamilyHandles.get(++i);
        milestoneHandle = columnFamilyHandles.get(++i);
        stateDiffHandle = columnFamilyHandles.get(++i);
        addressHandle = columnFamilyHandles.get(++i);
        approveeHandle = columnFamilyHandles.get(++i);
        bundleHandle = columnFamilyHandles.get(++i);
        obsoleteTagHandle = columnFamilyHandles.get(++i);
        tagHandle = columnFamilyHandles.get(++i);

        for (; ++i < columnFamilyHandles.size(); ) {
            db.dropColumnFamily(columnFamilyHandles.get(i));
        }
    }

}
