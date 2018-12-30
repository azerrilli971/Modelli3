package com.iota.iri.controllers;

import com.iota.iri.model.*;
import com.iota.iri.model.persistables.Address;
import com.iota.iri.model.persistables.Approvee;
import com.iota.iri.model.persistables.Bundle;
import com.iota.iri.model.persistables.ObsoleteTag;
import com.iota.iri.model.persistables.Tag;
import com.iota.iri.model.persistables.Transaction;
import com.iota.iri.storage.Indexable;
import com.iota.iri.storage.Persistable;
import com.iota.iri.storage.Tangle;
import com.iota.iri.utils.Converter;
import com.iota.iri.utils.Pair;

import java.util.*;

/**
 * Controller class for {@link Transaction} sets. A {@link TransactionViewModel} stores a {@link HashesViewModel} for
 * each component of the {@link Transaction} within it.
 */
public class TransactionViewModel {
    private final Transaction transaction;

    /**Length of a transaction object in trytes*/
    public static final int SIZE = 1604;
    private static final int TAG_SIZE_IN_BYTES = 17; // = ceil(81 TRITS / 5 TRITS_PER_BYTE)

    /**Total supply of IOTA available in the network. Used for ensuring a balanced ledger state and bundle balances*/
    public static final long SUPPLY = 2779530283277761L; // = (3^33 - 1) / 2

    /**The predefined offset position and size (in trits) for the varying components of a transaction object*/
    public static final int SIGNATURE_MESSAGE_FRAGMENT_TRINARY_OFFSET = 0;
    public static final int SIGNATURE_MESSAGE_FRAGMENT_TRINARY_SIZE = 6561;
    public static final int ADDRESS_TRINARY_OFFSET = SIGNATURE_MESSAGE_FRAGMENT_TRINARY_OFFSET + SIGNATURE_MESSAGE_FRAGMENT_TRINARY_SIZE;
    public static final int ADDRESS_TRINARY_SIZE = 243;
    public static final int VALUE_TRINARY_OFFSET = ADDRESS_TRINARY_OFFSET + ADDRESS_TRINARY_SIZE;
    public static final int VALUE_TRINARY_SIZE = 81;
    public static final int VALUE_USABLE_TRINARY_SIZE = 33;
    public static final int OBSOLETE_TAG_TRINARY_OFFSET = VALUE_TRINARY_OFFSET + VALUE_TRINARY_SIZE;
    public static final int OBSOLETE_TAG_TRINARY_SIZE = 81;
    public static final int TIMESTAMP_TRINARY_OFFSET = OBSOLETE_TAG_TRINARY_OFFSET + OBSOLETE_TAG_TRINARY_SIZE;
    public static final int TIMESTAMP_TRINARY_SIZE = 27;
    public static final int CURRENT_INDEX_TRINARY_OFFSET = TIMESTAMP_TRINARY_OFFSET + TIMESTAMP_TRINARY_SIZE;
    public static final int CURRENT_INDEX_TRINARY_SIZE = 27;
    public static final int LAST_INDEX_TRINARY_OFFSET = CURRENT_INDEX_TRINARY_OFFSET + CURRENT_INDEX_TRINARY_SIZE;
    public static final int LAST_INDEX_TRINARY_SIZE = 27;
    public static final int BUNDLE_TRINARY_OFFSET = LAST_INDEX_TRINARY_OFFSET + LAST_INDEX_TRINARY_SIZE;
    public static final int BUNDLE_TRINARY_SIZE = 243;
    public static final int TRUNK_TRANSACTION_TRINARY_OFFSET = BUNDLE_TRINARY_OFFSET + BUNDLE_TRINARY_SIZE;
    public static final int TRUNK_TRANSACTION_TRINARY_SIZE = 243;
    public static final int BRANCH_TRANSACTION_TRINARY_OFFSET = TRUNK_TRANSACTION_TRINARY_OFFSET + TRUNK_TRANSACTION_TRINARY_SIZE;
    public static final int BRANCH_TRANSACTION_TRINARY_SIZE = 243;

    public static final int TAG_TRINARY_OFFSET = BRANCH_TRANSACTION_TRINARY_OFFSET + BRANCH_TRANSACTION_TRINARY_SIZE;
    public static final int TAG_TRINARY_SIZE = 81;
    public static final int ATTACHMENT_TIMESTAMP_TRINARY_OFFSET = TAG_TRINARY_OFFSET + TAG_TRINARY_SIZE;
    public static final int ATTACHMENT_TIMESTAMP_TRINARY_SIZE = 27;
    public static final int ATTACHMENT_TIMESTAMP_LOWER_BOUND_TRINARY_OFFSET = ATTACHMENT_TIMESTAMP_TRINARY_OFFSET + ATTACHMENT_TIMESTAMP_TRINARY_SIZE;
    public static final int ATTACHMENT_TIMESTAMP_LOWER_BOUND_TRINARY_SIZE = 27;
    public static final int ATTACHMENT_TIMESTAMP_UPPER_BOUND_TRINARY_OFFSET = ATTACHMENT_TIMESTAMP_LOWER_BOUND_TRINARY_OFFSET + ATTACHMENT_TIMESTAMP_LOWER_BOUND_TRINARY_SIZE;
    public static final int ATTACHMENT_TIMESTAMP_UPPER_BOUND_TRINARY_SIZE = 27;
    private static final int NONCE_TRINARY_OFFSET = ATTACHMENT_TIMESTAMP_UPPER_BOUND_TRINARY_OFFSET + ATTACHMENT_TIMESTAMP_UPPER_BOUND_TRINARY_SIZE;
    public static final int NONCE_TRINARY_SIZE = 81;

    public static final int TRINARY_SIZE = NONCE_TRINARY_OFFSET + NONCE_TRINARY_SIZE;
    public static final int TRYTES_SIZE = TRINARY_SIZE / 3;

    public static final int ESSENCE_TRINARY_OFFSET = ADDRESS_TRINARY_OFFSET;
    public static final int ESSENCE_TRINARY_SIZE = ADDRESS_TRINARY_SIZE + VALUE_TRINARY_SIZE + OBSOLETE_TAG_TRINARY_SIZE + TIMESTAMP_TRINARY_SIZE + CURRENT_INDEX_TRINARY_SIZE + LAST_INDEX_TRINARY_SIZE;

    /**Stores the {@link HashesViewModel} for the {@link Transaction} components here*/
    private AddressViewModel address;
    private ApproveeViewModel approovers;
    private TransactionViewModel trunk;
    private TransactionViewModel branch;
    private final Hash hash;

    /**Transaction Types*/
    public static final int GROUP = 0; // transactions GROUP means that's it's a non-leaf node (leafs store transaction value)
    public static final int PREFILLED_SLOT = 1; // means that we know only hash of the tx, the rest is unknown yet: only another tx references that hash
    public static final int FILLED_SLOT = -1; //  knows the hash only coz another tx references that hash

    private byte[] trits;
    private int weightMagnitude;

    public void setWeightMagnitude(int weightMagnitude) {
        this.weightMagnitude = weightMagnitude;
    }

    public int getWeightMagnitude() {
        return weightMagnitude;
    }

    /**
     * Constructor for a {@link Transaction} set controller interface. This controller is used to interact with and
     * manipulate a provided {@link Transaction} set.
     *
     * @param transaction {@link Transaction} set that the {@link TransactionViewModel} will be created for
     * @param hash The {@link Hash} identifier of the {@link Transaction} set
     */
    public TransactionViewModel(final Transaction transaction, Hash hash) {
        this.transaction = transaction == null || transaction.getBytes() == null ? new Transaction(): transaction;
        this.hash = hash == null? Hash.NULL_HASH: hash;
        weightMagnitude = this.hash.trailingZeros();
    }

    /**
     * Constructor for a {@link Transaction} set controller interface. A new {@link Transaction} set is created from
     * the provided trit array, if the array is of the correct size. If it is not the correct size, a new byte
     * array of the correct size is created, the trit array is copied into it, and the bytes are stored in the new
     * {@link Transaction} set. This {@link Transaction} set is then indexed by the provided {@link Hash} identifier.
     *
     * @param trits The input trits that the {@link Transaction} and {@link TransactionViewModel} will be created from.
     * @param hash The {@link TransactionHash} identifier of the {@link Transaction} set
     */
    public TransactionViewModel(final byte[] trits, Hash hash) {
        transaction = new Transaction();

        if(trits.length == 8019) {
            this.trits = new byte[trits.length];
            System.arraycopy(trits, 0, this.trits, 0, trits.length);
            transaction.setBytes(Converter.allocateBytesForTrits(trits.length));
            Converter.bytes(trits, 0, transaction.getBytes(), 0, trits.length);

            transaction.setValidity(0);
            transaction.setArrivalTime(0);
        } else {
            transaction.setBytes(new byte[SIZE]);
            System.arraycopy(trits, 0, transaction.getBytes(), 0, SIZE);
        }

        this.hash = hash;
        weightMagnitude = this.hash.trailingZeros();
        transaction.setType(FILLED_SLOT);
    }

    /**
     * Creates a new controller for a {@link Transaction} set referenced by a given {@link Hash} identifier.
     * The controller will be created and its metadata filled provided the {@link Transaction} object exists in the
     * database.
     *
     * @param tangle The tangle reference for the database
     * @param hash The {@link Hash} identifier to search with
     * @return The {@link TransactionViewModel} with its Metadata filled in.
     * @throws Exception Thrown if there is an error loading the {@link Transaction} object from the database
     */
    public static TransactionViewModel fromHash(Tangle tangle, final Hash hash) throws Exception {
        TransactionViewModel transactionViewModel = new TransactionViewModel((Transaction) tangle.load(Transaction.class, hash), hash);
        fillMetadata(tangle, transactionViewModel);
        return transactionViewModel;
    }

    /**
     * This method checks the {@link com.iota.iri.storage.rocksDB.RocksDBPersistenceProvider} to determine if the
     * {@link Transaction} object might exist in the database. If it definitively does not exist, it will return False.
     *
     * @param tangle The tangle reference for the database
     * @param hash The {@link Hash} identifier of the object you are looking for
     * @return True if the key might exist in the database, False if it definitively does not
     * @throws Exception Thrown if there is an error checking the database
     */
    public static boolean mightExist(Tangle tangle, Hash hash) throws Exception {
        return tangle.maybeHas(Transaction.class, hash);
    }

    /**
     * Determines whether the {@link Transaction} object exists in the database or not.
     *
     * @param tangle The tangle reference for the database.
     * @param hash The {@link Hash} identifier for the {@link Transaction} object
     * @return True if the transaction exists in the database, False if not
     * @throws Exception Thrown if there is an error determining if the transaction exists or not
     */
    public static boolean exists(Tangle tangle, Hash hash) throws Exception {
        return tangle.exists(Transaction.class, hash);
    }
    
    /**
     * Returns the total number of {@link Transaction} objects stored in a database.
     *
     * @param tangle The tangle reference for the database.
     * @return The integer count of total {@link Transaction} objects in the database
     * @throws Exception Thrown if there is an error getting the count of objects
     */
    public static int getNumberOfStoredTransactions(Tangle tangle) throws Exception {
        return tangle.getCount(Transaction.class).intValue();
    }

    /**
     * Converts the given byte array to the a new trit array of length {@value TRINARY_SIZE}.
     * @param transactionBytes The byte array to be converted to trits
     * @return The trit conversion of the byte array
     */
    public static byte[] trits(byte[] transactionBytes) {
        byte[] trits;
        trits = new byte[TRINARY_SIZE];
        if(transactionBytes != null) {
            Converter.getTrits(transactionBytes, trits);
        }
        return trits;
    }

    public static Set<Indexable> getMissingTransactions(Tangle tangle) throws Exception {
        return tangle.keysWithMissingReferences(Approvee.class, Transaction.class);
    }

    /**
     * Creates a new controller using a byte array that will be converted to a {@link Hash} identifier. The new
     * {@link TransactionViewModel} is created for the {@link Transaction} object referenced by this {@link Hash}
     * identifier, provided the {@link Transaction} exists in the database.
     *
     * @param tangle The tangle reference for the database
     * @param hash The source that the {@link Hash} identifier will be created from, and the {@link Transaction} object
     *             will be fetched from the database from
     * @return The {@link TransactionViewModel} with its Metadata filled in.
     * @throws Exception Thrown if the database fails to find the {@link Transaction} object
     */
    public static TransactionViewModel find(Tangle tangle, byte[] hash) throws Exception {
        TransactionViewModel transactionViewModel = new TransactionViewModel((Transaction) tangle.find(Transaction.class, hash), HashFactory.TRANSACTION.create(hash));
        fillMetadata(tangle, transactionViewModel);
        return transactionViewModel;
    }

    /**
     * Fetches the first persistable {@link Transaction} object from the database and generates a new
     * {@link TransactionViewModel} from it. If no objects exist in the database, it will return null.
     *
     * @param tangle the tangle reference for the database.
     * @return The new {@link TransactionViewModel}.
     * @throws Exception Thrown if the database fails to return a first object.
     */
    public static TransactionViewModel first(Tangle tangle) throws Exception {
        Pair<Indexable, Persistable> transactionPair = tangle.getFirst(Transaction.class, Hash.class);
        if(transactionPair != null && transactionPair.hi != null) {
            return new TransactionViewModel((Transaction) transactionPair.hi, (Hash) transactionPair.low);
        }
        return null;
    }

    /**
     * Updates a set of {@link Transaction} objects to solid. Used by the {@link com.iota.iri.TransactionValidator} to
     * solidify groups of analyzed {@link Transaction} sets.
     *
     * @param tangle The tangle reference for the database.
     * @param analyzedHashes A list of {@link TransactionHash} identifiers to be checked for solidity.
     * @throws Exception Thrown if there is a failure to generate a controller, or if there is a failure to update
     */
    public static void updateSolidTransactions(Tangle tangle, final Set<Hash> analyzedHashes) throws Exception {
        Iterator<Hash> hashIterator = analyzedHashes.iterator();
        TransactionViewModel transactionViewModel;
        while(hashIterator.hasNext()) {
            transactionViewModel = TransactionViewModel.fromHash(tangle, hashIterator.next());

            transactionViewModel.updateHeights(tangle);

            if(!transactionViewModel.isSolid()) {
                transactionViewModel.updateSolid(true);
                transactionViewModel.update(tangle, "solid|height");
            }
        }
    }

    /**
     * Populates the meta data of the {@link TransactionViewModel}. If the controller {@link Hash} identifier is
     * null, it will return with no response. If the {@link Transaction} object has not been parsed, and the
     * {@link TransactionViewModel} type is <tt>FILLED_SLOT</tt>, the saved metadata batch will be saved to the
     * database.
     *
     * @param tangle The tangle reference for the database.
     * @param transactionViewModel The {@link TransactionViewModel} whose Metadata is to be filled.
     * @throws Exception Thrown if the database fails to save the batch of data.
     */

    public static void fillMetadata(Tangle tangle, TransactionViewModel transactionViewModel) throws Exception {
        if (Hash.NULL_HASH.equals(transactionViewModel.getHash())) {
            return;
        }
        if(transactionViewModel.getType() == FILLED_SLOT && !transactionViewModel.transaction.getParsed()) {
            tangle.saveBatch(transactionViewModel.getMetadataSaveBatch());
        }
    }

    /**
     * This method updates the metadata contained in the {@link Transaction} object, and updates the object in the
     * database. First, all the most recent {@link Hash} identifiers are fetched to make sure the object's metadata is
     * up to date. Then it checks if the current {@link TransactionHash} is null. If it is, then the method immediately
     * returns false, and if not, it attempts to update the {@link Transaction} object and the referencing {@link Hash}
     * identifier in the database.
     *
     * @param tangle The tangle reference for the database
     * @param item The string identifying the purpose of the update
     * @return True if the update was successful, False if it failed
     * @throws Exception Thrown if any of the metadata fails to fetch, or if the database update fails
     */
    public boolean update(Tangle tangle, String item) throws Exception {
        getAddressHash();
        getTrunkTransactionHash();
        getBranchTransactionHash();
        getBundleHash();
        getTagValue();
        getObsoleteTagValue();
        setAttachmentData();
        setMetadata();
        if(hash.equals(Hash.NULL_HASH)) {
            return false;
        }
        return tangle.update(transaction, hash, item);
    }

    /**
     * Retrieves the {@link TransactionViewModel} for the branch {@link Transaction} object referenced by this
     * {@link TransactionViewModel}. If the controller doesn't already exist, a new one is created from the branch
     * transaction {@link Hash} identifier.
     *
     * @param tangle The tangle reference for the database.
     * @return The branch transaction {@link TransactionViewModel}
     * @throws Exception Thrown if no branch is found when creating the branch {@link TransactionViewModel}
     */
    public TransactionViewModel getBranchTransaction(Tangle tangle) throws Exception {
        if(branch == null) {
            branch = TransactionViewModel.fromHash(tangle, getBranchTransactionHash());
        }
        return branch;
    }

    /**
     * Retrieves the {@link TransactionViewModel} for the trunk {@link Transaction} object referenced by this
     * {@link TransactionViewModel}. If the controller doesn't already exist, a new one is created from the trunk
     * transaction {@link Hash} identifier.
     *
     * @param tangle The tangle reference for the database.
     * @return The trunk transaction {@link TransactionViewModel}
     * @throws Exception Thrown if no trunk is found when creating the trunk {@link TransactionViewModel}
     */
    public TransactionViewModel getTrunkTransaction(Tangle tangle) throws Exception {
        if(trunk == null) {
            trunk = TransactionViewModel.fromHash(tangle, getTrunkTransactionHash());
        }
        return trunk;
    }

    /**
     * @return The trits stored in the {@link TransactionViewModel} . If the trits aren't available in the controller,
     * the trits are generated from the transaction object and stored in the controller {@link #trits()} variable.
     */
    public synchronized byte[] trits() {
        if (trits == null){
            trits = trits(transaction.getBytes());
        }
        return trits;
    }

    /**
     * Deletes the {@link Transaction} object from the database
     * @param tangle The tangle reference for the database
     * @throws Exception Thrown if there is an error removing the object
     */
    public void delete(Tangle tangle) throws Exception {
        tangle.delete(Transaction.class, hash);
    }

    /**
     * Stores the {@link Transaction} object to the tangle, including the metadata and indexing based on {@link Bundle},
     * {@link Address}, {@link Tag}, {@link #trunk} and {@link #branch}.
     *
     * @return The list of {@link Hash} objects indexed by the {@link TransactionHash} identifier. Returns False if
     *         there is a problem populating the list.
     */

    public List<Pair<Indexable, Persistable>> getMetadataSaveBatch() {
        List<Pair<Indexable, Persistable>> hashesList = new ArrayList<>();
        hashesList.add(new Pair<>(getAddressHash(), new Address(hash)));
        hashesList.add(new Pair<>(getBundleHash(), new Bundle(hash)));
        hashesList.add(new Pair<>(getBranchTransactionHash(), new Approvee(hash)));
        hashesList.add(new Pair<>(getTrunkTransactionHash(), new Approvee(hash)));
        hashesList.add(new Pair<>(getObsoleteTagValue(), new ObsoleteTag(hash)));
        hashesList.add(new Pair<>(getTagValue(), new Tag(hash)));
        setAttachmentData();
        setMetadata();
        return hashesList;
    }

    /**
     * Fetches a list of all {@link Transaction} component and {@link Hash} identifier pairs from the stored metadata.
     * The method then ensures that the {@link Transaction#getBytes()} are present before adding the {@link Transaction} and
     * {@link Hash} identifier to the already compiled list of {@link Transaction} components.
     *
     * @return A complete list of all {@link Transaction} component objects paired with their {@link Hash} identifiers
     * @throws Exception Thrown if the metadata fails to fetch, or if the bytes are not retrieved correctly
     */
    public List<Pair<Indexable, Persistable>> getSaveBatch() throws Exception {
        List<Pair<Indexable, Persistable>> hashesList = new ArrayList<>();
        hashesList.addAll(getMetadataSaveBatch());
        getBytes();
        hashesList.add(new Pair<>(hash, transaction));
        return hashesList;
    }

    /**
     * Fetches the next indexed persistable {@link Transaction} object from the database and generates a new
     * {@link TransactionViewModel} from it. If no objects exist in the database, it will return null.
     *
     * @param tangle the tangle reference for the database.
     * @return The new {@link TransactionViewModel}.
     * @throws Exception Thrown if the database fails to return a next object.
     */
    public TransactionViewModel next(Tangle tangle) throws Exception {
        Pair<Indexable, Persistable> transactionPair = tangle.next(Transaction.class, hash);
        if(transactionPair != null && transactionPair.hi != null) {
            return new TransactionViewModel((Transaction) transactionPair.hi, (Hash) transactionPair.low);
        }
        return null;
    }

    /**
     * This method fetches the saved batch of metadata and orders them into a list of {@link Hash} objects and
     * {@link Hash} identifier pairs. If the {@link Hash} identifier of the {@link Transaction} is null, or the database
     * already contains the {@link Transaction}, then the method returns False. Otherwise, the method tries to store the
     * {@link Transaction} batch into the database.
     *
     * @param tangle The tangle reference for the database.
     * @return True if the {@link Transaction} is stored, False if not.
     * @throws Exception Thrown if there is an error fetching the batch or storing in the database.
     */
    public boolean store(Tangle tangle) throws Exception {
        if (hash.equals(Hash.NULL_HASH) || exists(tangle, hash)) {
            return false;
        }

        List<Pair<Indexable, Persistable>> batch = getSaveBatch();
        if (exists(tangle, hash)) {
            return false;
        }
        return tangle.saveBatch(batch);
    }

    /**
     * Gets the {@link ApproveeViewModel} of a {@link Transaction}. If the current {@link ApproveeViewModel} is null,
     * a new one is created using the transaction {@link Hash} identifier.
     *
     * An {@link Approvee} is a transaction in the tangle that references, and therefore approves, this transaction
     * directly.
     *
     * @param tangle The tangle reference for the database
     * @return The {@link ApproveeViewModel}
     * @throws Exception Thrown if there is a failure to create a controller from the transaction hash
     */
    public ApproveeViewModel getApprovers(Tangle tangle) throws Exception {
        if(approovers == null) {
            approovers = ApproveeViewModel.load(tangle, hash);
        }
        return approovers;
    }

    /*
    /**
     * Gets the {@link Transaction#type}. The type can be one of 3:
     * <ul>
     *     <li>PREFILLED_SLOT: 1</li>
     *     <li>FILLED_SLOT: -1</li>
     *     <li>GROUP: 0</li>
     * </ul>
     *
     * @return The current type of the transaction.
     */
    public final int getType() {
        return transaction.getType();
    }

    /*
    /**
     * Sets the {@link Transaction#arrivalTime}.
     * @param time The time to be set in the {@link Transaction}
     */
    public void setArrivalTime(long time) {
        transaction.setArrivalTime(time);
    }

   // /**@return The {@link Transaction#arrivalTime}*/
    public long getArrivalTime() {
        return transaction.getArrivalTime();
    }

    /**
     * Gets the stored {@link Transaction#getBytes()} } If the {@link Transaction#getBytes()} are null, a new byte
     * array is created and stored from the {@link #trits}. If the {@link #trits} are also null, then a null byte array
     * is returned.
     *
     * @return The stored {@link Transaction#getBytes()} array
     */
    public byte[] getBytes() {
        if(transaction.getBytes() == null || transaction.getBytes().length != SIZE) {
            transaction.setBytes(new byte[SIZE]);
            if(trits != null) {
                Converter.bytes(trits(), 0, transaction.getBytes(), 0, trits().length);
            }
        }
        return transaction.getBytes();
    }

    /**@return The transaction {@link Hash} identifier*/
    public Hash getHash() {
        return hash;
    }

    /**
     * Gets the {@link AddressViewModel} associated with this {@link Transaction}.
     *
     * @param tangle The tangle reference for the database.
     * @return The {@link AddressViewModel} of the {@link Transaction}.
     * @throws Exception If the address cannot be found in the database, an exception is thrown.
     */
    public AddressViewModel getAddress(Tangle tangle) throws Exception {
        if(address == null) {
            address = AddressViewModel.load(tangle, getAddressHash());
        }
        return address;
    }

    /**
     * Gets the {@link TagViewModel} associated with this {@link Transaction}.
     *
     * @param tangle The tangle reference for the database.
     * @return The {@link TagViewModel} of the {@link Transaction}.
     * @throws Exception If the address cannot be found in the database, an exception is thrown.
     */
    public TagViewModel getTag(Tangle tangle) throws Exception {
        return TagViewModel.load(tangle, getTagValue());
    }

    /**
     * Gets the {@link AddressHash} identifier of a {@link Transaction}.
     *
     * @return The {@link AddressHash} identifier.
     */
    public Hash getAddressHash() {
        if(transaction.getAddress() == null) {
            transaction.setAddress(HashFactory.ADDRESS.create(trits(), ADDRESS_TRINARY_OFFSET));
        }
        return transaction.getAddress();
    }

    /**
     * Gets the {@link ObsoleteTagHash} identifier of a {@link Transaction}.
     *
     * @return The {@link ObsoleteTagHash} identifier.
     */
    public Hash getObsoleteTagValue() {
        if(transaction.getObsoleteTag() == null) {
            byte[] tagBytes = Converter.allocateBytesForTrits(OBSOLETE_TAG_TRINARY_SIZE);
            Converter.bytes(trits(), OBSOLETE_TAG_TRINARY_OFFSET, tagBytes, 0, OBSOLETE_TAG_TRINARY_SIZE);

            transaction.setObsoleteTag(HashFactory.OBSOLETETAG.create(tagBytes, 0, TAG_SIZE_IN_BYTES));
        }
        return transaction.getObsoleteTag();
    }

    /**
     * Gets the {@link BundleHash} identifier of a {@link Transaction}.
     *
     * @return The {@link BundleHash} identifier.
     */
    public Hash getBundleHash() {
        if(transaction.getBundle()== null) {
            transaction.setBundle(HashFactory.BUNDLE.create(trits(), BUNDLE_TRINARY_OFFSET));
        }
        return transaction.getBundle();
    }

    /**
     * Gets the trunk {@link TransactionHash} identifier of a {@link Transaction}.
     *
     * @return The trunk {@link TransactionHash} identifier.
     */
    public Hash getTrunkTransactionHash() {
        if(transaction.getTrunk() == null) {
            transaction.setTrunk(HashFactory.TRANSACTION.create(trits(), TRUNK_TRANSACTION_TRINARY_OFFSET));
        }
        return transaction.getTrunk();
    }

    /**
     * Gets the branch {@link TransactionHash} identifier of a {@link Transaction}.
     *
     * @return The branch {@link TransactionHash} identifier.
     */
    public Hash getBranchTransactionHash() {
        if(transaction.getBranch() == null) {
            transaction.setBranch(HashFactory.TRANSACTION.create(trits(), BRANCH_TRANSACTION_TRINARY_OFFSET));
        }
        return transaction.getBranch();
    }

    /**
     * Gets the {@link TagHash} identifier of a {@link Transaction}.
     *
     * @return The {@link TagHash} identifier.
     */
    public Hash getTagValue() {
        if(transaction.getTag()== null) {
            byte[] tagBytes = Converter.allocateBytesForTrits(TAG_TRINARY_SIZE);
            Converter.bytes(trits(), TAG_TRINARY_OFFSET, tagBytes, 0, TAG_TRINARY_SIZE);
            transaction.setTag(HashFactory.TAG.create(tagBytes, 0, TAG_SIZE_IN_BYTES));
        }
        return transaction.getTag();
    }

    /**
     * Gets the {@link Transaction attachmentTimestamp}. The <tt>Attachment Timestapm</tt> is used to show when
     * a transaction has been attached to the database.
     *
     * @return The {@link Transaction attachmentTimestamp}
     */
    public long getAttachmentTimestamp() { return transaction.getAttachmentTimestamp(); }

    /*
    /**
     * Gets the {@link Transaction#attachmentTimestampLowerBound}. The <tt>Attachment Timestamp Lower Bound</tt>
     * is the earliest timestamp a transaction can have.
     *
     * @return The {@link Transaction#attachmentTimestampLowerBound}
     */
    public long getAttachmentTimestampLowerBound() {
        return transaction.getAttachmentTimestampLowerBound();
    }

    /*
    /**
     * Gets the {@link Transaction#attachmentTimestampUpperBound}. The <tt>Attachment Timestamp Upper Bound</tt>
     * is the maximum timestamp a transaction can have.
     *
     * @return The {@link Transaction#attachmentTimestampUpperBound}
     */
    public long getAttachmentTimestampUpperBound() {
        return transaction.getAttachmentTimestampUpperBound();
    }

    /**@return The {@link Transaction#getValue()}*/
    public long value() {
        return transaction.getValue();
    }

    /*
    /**
     * Updates the {@link Transaction#validity} in the database.
     *
     * The validity can be one of three states:
     * <tt>1: Valid; -1: Invalid; 0: Unknown</tt>
     * @param tangle The tangle reference for the database
     * @param validity The state of validity that the {@link Transaction} will be updated to
     * @throws Exception Thrown if there is an error with the update
     */
    public void setValidity(Tangle tangle, int validity) throws Exception {
        if(transaction.getValidity() != validity) {
            transaction.setValidity(validity);
            update(tangle, "validity");
        }
    }

    // /**@return The current stored {@link Transaction#validity}*/
    public int getValidity() {
        return transaction.getValidity();
    }

    /**@return The {@link Transaction#getCurrentIndex()} in its bundle*/
    public long getCurrentIndex() {
        return transaction.getCurrentIndex();
    }

    /**
     * Creates an array copy of the signature message fragment of the {@link Transaction} and returns it.
     * @return The signature message fragment in array format.
     */
    public byte[] getSignature() {
        return Arrays.copyOfRange(trits(), SIGNATURE_MESSAGE_FRAGMENT_TRINARY_OFFSET, SIGNATURE_MESSAGE_FRAGMENT_TRINARY_SIZE);
    }

    /**@return The stored {@link Transaction# timestamp}*/
    public long getTimestamp() {
        return transaction.getTimestamp();
    }

    /**
     * Creates a byte array of the size {@value NONCE_TRINARY_SIZE} and copies the nonce section of the transaction
     * trits into the array. This array is then returned.
     *
     * @return A byte array containing the nonce of the transaction
     */
    public byte[] getNonce() {
        byte[] nonce = Converter.allocateBytesForTrits(NONCE_TRINARY_SIZE);
        Converter.bytes(trits(), NONCE_TRINARY_OFFSET, nonce, 0, trits().length);
        return nonce;
    }

    /**@return The {@link Transaction lastIndex} of the transaction bundle*/
    public long lastIndex() {
        return transaction.getLastIndex();
    }

    /*
    /**
     * Fetches the {@link Transaction#tag}, and converts the transaction trits for the
     * {@link Transaction#attachmentTimestamp}, the {@link Transaction#attachmentTimestampLowerBound}, and the
     * {@link Transaction#attachmentTimestampUpperBound} to long values.The method then sets these values to the
     * {@link TransactionViewModel} metadata.
     */
    public void setAttachmentData() {
        getTagValue();
        transaction.setAttachmentTimestamp(Converter.longValue(trits(), ATTACHMENT_TIMESTAMP_TRINARY_OFFSET, ATTACHMENT_TIMESTAMP_TRINARY_SIZE));
        transaction.setAttachmentTimestampLowerBound(Converter.longValue(trits(), ATTACHMENT_TIMESTAMP_LOWER_BOUND_TRINARY_OFFSET, ATTACHMENT_TIMESTAMP_LOWER_BOUND_TRINARY_SIZE));
        transaction.setAttachmentTimestampUpperBound(Converter.longValue(trits(), ATTACHMENT_TIMESTAMP_UPPER_BOUND_TRINARY_OFFSET, ATTACHMENT_TIMESTAMP_UPPER_BOUND_TRINARY_SIZE));

    }

    /*
    /**
     * Converts the {@link Transaction#value}, {@link Transaction#timestamp}, {@link Transaction#currentIndex} and
     * {@link Transaction#lastIndex} from trits to long values and assigns them to the {@link TransactionViewModel}
     * metadata. The method then determines if the {@link Transaction#bytes} are null or not. If so the
     * {@link Transaction#type} is set to {@link #PREFILLED_SLOT}, and if not it is set to {@link #FILLED_SLOT}.
     */
    public void setMetadata() {
        transaction.setValue(Converter.longValue(trits(), VALUE_TRINARY_OFFSET, VALUE_USABLE_TRINARY_SIZE));
        transaction.setTimestamp(Converter.longValue(trits(), TIMESTAMP_TRINARY_OFFSET, TIMESTAMP_TRINARY_SIZE));
        transaction.setCurrentIndex(Converter.longValue(trits(), CURRENT_INDEX_TRINARY_OFFSET, CURRENT_INDEX_TRINARY_SIZE));
        transaction.setLastIndex(Converter.longValue(trits(), LAST_INDEX_TRINARY_OFFSET, LAST_INDEX_TRINARY_SIZE));
        transaction.setType(transaction.getBytes() == null ? TransactionViewModel.PREFILLED_SLOT : TransactionViewModel.FILLED_SLOT);
    }


    /**
     * Updates the {@link Transaction#getSolid()} } value of the referenced {@link Transaction} object.
     *
     * Used by the {@link com.iota.iri.TransactionValidator} to quickly set the solidity of a {@link Transaction} set.
     *
     * @param solid The solidity of the transaction in the database
     * @return True if the {@link Transaction#getSolid()} } has been updated, False if not.
     */
    public boolean updateSolid(boolean solid) {
        if(solid != transaction.getSolid()) {
            transaction.setSolid(solid);
            return true;
        }
        return false;
    }

    /**@return True if {@link Transaction#getSolid()} } is True (exists in the database), False if not*/
    public boolean isSolid() {
        return transaction.getSolid();
    }

    // /**@return The {@link Transaction#snapshot} index*/
    public int snapshotIndex() {
        return transaction.getSnapshot();
    }

    /*
    /**
     * Sets the current {@link Transaction#snapshot} index.
     *
     * This is used to set a milestone transactions index.
     *
     * @param tangle The tangle reference for the database.
     * @param index The new index to be attached to the {@link Transaction} object
     * @throws Exception Thrown if the database update does not return correctly
     */
    public void setSnapshot(Tangle tangle, final int index) throws Exception {
        if ( index != transaction.getSnapshot() ) {
            transaction.setSnapshot(index);
            update(tangle, "snapshot");
        }
    }

    /**
     * This method sets the {@link Transaction#getMilestone()} flag.
     *
     * It gets automatically called by the {@link com.iota.iri.MilestoneTracker} and marks transactions that represent
     * a milestone accordingly. It first checks if the {@link Transaction#getMilestone()} } flag has changed and if so, it
     * issues a database update.
     *
     * @param tangle Tangle instance which acts as a database interface
     * @param isMilestone True if the {@link Transaction} is a milestone and False if not
     * @throws Exception Thrown if there is an error while saving the changes to the database
     */
    public void isMilestone(Tangle tangle, final boolean isMilestone) throws Exception {
        if (isMilestone != transaction.getMilestone()) {
            transaction.setMilestone(isMilestone);
            update(tangle, "milestone");
        }
    }

    /**
     * This method gets the {@link Transaction#getMilestone()} }.
     *
     * The {@link Transaction#getMilestone()} } flag indicates if the {@link Transaction} is a coordinator issued milestone. It
     * allows us to differentiate the two types of transactions (normal transactions / milestones) very fast and
     * efficiently without issuing further database queries or even full verifications of the signature. If it is set to
     * true one can for example use the snapshotIndex() method to retrieve the corresponding
     * {@link MilestoneViewModel} object.
     *
     * @return true if the {@link Transaction} is a milestone and false otherwise
     */
    public boolean isMilestone() {
        return transaction.getMilestone();
    }

    /**@return The current {@link Transaction#getHeight()} }*/
    public long getHeight() {
        return transaction.getHeight();
    }

    /**
     * Updates the {@link Transaction#getHeight()} }.
     * @param height The new height of the {@link Transaction}
     */
    private void updateHeight(long height){
        transaction.setHeight(height);
    }

    /**
     *
     * @param tangle
     * @throws Exception
     */
    public void updateHeights(Tangle tangle) throws Exception {
        TransactionViewModel transactionVM = this;
        TransactionViewModel trunk2 = this.getTrunkTransaction(tangle);
        Stack<Hash> transactionViewModels = new Stack<>();
        transactionViewModels.push(transactionVM.getHash());
        while(trunk2.getHeight() == 0 && trunk2.getType() != PREFILLED_SLOT && !trunk2.getHash().equals(Hash.NULL_HASH)) {
            transactionVM = trunk2;
            trunk2 = transactionVM.getTrunkTransaction(tangle);
            transactionViewModels.push(transactionVM.getHash());
        }
        while(transactionViewModels.size() != 0) {
            transactionVM = TransactionViewModel.fromHash(tangle, transactionViewModels.pop());
            long currentHeight = transactionVM.getHeight();
            if(Hash.NULL_HASH.equals(trunk2.getHash()) && trunk2.getHeight() == 0
                    && !Hash.NULL_HASH.equals(transactionVM.getHash())) {
                if(currentHeight != 1L ){
                    transactionVM.updateHeight(1L);
                    transactionVM.update(tangle, "height");
                }
            } else if ( trunk2.getType() != PREFILLED_SLOT && transactionVM.getHeight() == 0){
                long newHeight = 1L + trunk2.getHeight();
                if(currentHeight != newHeight) {
                    transactionVM.updateHeight(newHeight);
                    transactionVM.update(tangle, "height");
                }
            } else {
                break;
            }
            trunk2 = transactionVM;
        }
    }

    /*
    /**
     * Updates the {@link Transaction#sender}.
     * @param sender The sender of the {@link Transaction}
     */
    public void updateSender(String sender){
        transaction.setSender(sender);
    }

    // /**@return The {@link Transaction#sender}*/
    public String getSender() {
        return transaction.getSender();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TransactionViewModel other = (TransactionViewModel) o;
        return Objects.equals(getHash(), other.getHash());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getHash());
    }

    /**
     * This method creates a human readable string representation of the transaction.
     *
     * It can be used to directly append the transaction in error and debug messages.
     *
     * @return human readable string representation of the transaction
     */
    @Override
    public String toString() {
        return "transaction " + hash.toString();
    }
}
