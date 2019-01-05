package com.iota.iri.model.persistables;

import com.iota.iri.model.Hash;
import com.iota.iri.model.HashFactory;
import com.iota.iri.storage.Persistable;
import org.apache.commons.lang3.ArrayUtils;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by paul on 3/8/17 for iri.
 */
public class Hashes implements Persistable {
    private Set<Hash> set = new LinkedHashSet<>();
    private static final byte DELIMITER = ",".getBytes()[0];

    //getter e setter per hash
    public Set<Hash> getSet() { return set;}
    public void setSet (Set<Hash> newSet) {set = newSet;}


    public byte[] bytes() {
        return set.parallelStream()
                .map(Hash::bytes)
                .reduce((a,b) -> ArrayUtils.addAll(ArrayUtils.add(a, DELIMITER), b))
                .orElse(new byte[0]);
    }

    public void read(byte[] bytes) {
        if(bytes != null) {
            set = new LinkedHashSet<>(bytes.length / (1 + Hash.SIZE_IN_BYTES) + 1);
            for (int i = 0; i < bytes.length; i += 1 + Hash.SIZE_IN_BYTES) {
                set.add(HashFactory.TRANSACTION.create(bytes, i, Hash.SIZE_IN_BYTES));
            }
        }
    }

    @Override
    public byte[] metadata() {
        return new byte[0];
    }

    @Override
    public void readMetadata(byte[] bytes) {
        //Random override
    }

    @Override
    public boolean merge() {
        return true;
    }
}
