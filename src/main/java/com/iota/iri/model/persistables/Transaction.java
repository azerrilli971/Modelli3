package com.iota.iri.model.persistables;

import com.iota.iri.controllers.TransactionViewModel;
import com.iota.iri.model.Hash;
import com.iota.iri.model.HashFactory;
import com.iota.iri.storage.Persistable;
import com.iota.iri.utils.Serializer;

import java.nio.ByteBuffer;

public class Transaction implements Persistable {
    public static final int SIZE = 1604;

    /**
     * Bitmask used to access and store the solid flag.
     */
    public static final int IS_SOLID_BITMASK = 0b01;

    /**
     * Bitmask used to access and store the milestone flag.
     */
    public static final int IS_MILESTONE_BITMASK = 0b10;

    private byte[] bytes;

    private Hash address;
    private Hash bundle;
    private Hash trunk;
    private Hash branch;
    private Hash obsoleteTag;
    private long value;
    private long currentIndex;
    private long lastIndex;
    private long timestamp;

    private Hash tag;
    private long attachmentTimestamp;
    private long attachmentTimestampLowerBound;
    private long attachmentTimestampUpperBound;

    private int validity = 0;
    private int type = TransactionViewModel.PREFILLED_SLOT;
    private long arrivalTime = 0;

    private boolean parsed = false;

    //get e set arrivalTime
    public long getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(long arrivalTime) { this.arrivalTime = arrivalTime; }

    //get e set attachmentTimestamp
    public long getAttachmentTimestamp(){return attachmentTimestamp;}

    public void setAttachmentTimestamp( long newTimestamp) {this.attachmentTimestamp = newTimestamp;}
    //get e set type
    public int getType() { return type; }
    public void setType(int type) { this.type = type; }

    //get e set validity
    public int getValidity() { return validity; }
    public void setValidity(int validity) { this.validity = validity; }

    //get e set attachmentTimestampUpperBound
    public long getAttachmentTimestampUpperBound() {return attachmentTimestampUpperBound; }
    public void setAttachmentTimestampUpperBound(long attachmentTimestampUpperBound) {
        this.attachmentTimestampUpperBound = attachmentTimestampUpperBound; }

    //get e set attachmentTimestampLowerBound
    public long getAttachmentTimestampLowerBound() { return attachmentTimestampLowerBound; }
    public void setAttachmentTimestampLowerBound(long attachmentTimestampLowerBound) {
        this.attachmentTimestampLowerBound = attachmentTimestampLowerBound; }


     // getter e setter tag

    public Hash getTag(){return tag;}
    public void setTag( Hash newTag) {this.tag = newTag;}
    //get e set parsed
    public void setParsed(boolean parsed) {
        this.parsed = parsed;
    }
    public boolean getParsed(){
        return parsed;
    }

    //get e set trunk
    public Hash getTrunk() {
        return trunk;
    }
    public void setTrunk(Hash trunk) {
        this.trunk = trunk;
    }

    //getter e setter lastIndex
    public long getLastIndex() {
        return lastIndex;
    }
    public void setLastIndex( long newIndex) {
        this.lastIndex = newIndex;
    }

    //getter setter timestamp
    public long getTimestamp(){return timestamp;}
    public void setTimestamp (long newTimeStamp) {this.timestamp = newTimeStamp;}

    //getter e setter Byte
    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    //getter and setter address

    public Hash getAddress() {
        return address;
    }

    public void setAddress(Hash address) {
        this.address = address;
    }

    //getter and setter bundle


    public Hash getBundle() {
        return bundle;
    }

    public void setBundle(Hash bundle) {
        this.bundle = bundle;
    }

    //getter e setter Branch


    public Hash getBranch() {
        return branch;
    }

    public void setBranch(Hash branch) {
        this.branch = branch;
    }

    //getter e setter ObsoleteTag


    public Hash getObsoleteTag() {
        return obsoleteTag;
    }

    public void setObsoleteTag(Hash obsoleteTag) {
        this.obsoleteTag = obsoleteTag;
    }

    //getter e setter Value


    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    //Getter e setter CurrentIndex


    public long getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(long currentIndex) {
        this.currentIndex = currentIndex;
    }

    private boolean solid = false;

    //get e set solid
    public void setSolid(boolean solid) {
        this.solid = solid;
    }
    public boolean getSolid(){return solid;}
    /**
     * This flag indicates if the transaction is a coordinator issued milestone.
     */
    private boolean milestone = false;

    public void setMilestone(boolean milestone) {
        this.milestone = milestone;
    }

    public boolean getMilestone() {return milestone;}

    private long height = 0;

    public void setHeight(long height) {
        this.height = height;
    }

    public long getHeight() {
        return height;
    }

    private String sender = "";
    private int snapshot;

    //get e set snapshot
    public int getSnapshot() { return snapshot; }
    public void setSnapshot(int snapshot) { this.snapshot = snapshot; }

    //get e set sender
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public byte[] bytes() {
        return bytes;
    }

    public void read(byte[] bytes) {
        if(bytes != null) {
            this.bytes = new byte[SIZE];
            System.arraycopy(bytes, 0, this.bytes, 0, SIZE);
            this.type = TransactionViewModel.FILLED_SLOT;
        }
    }

    @Override
    public byte[] metadata() {
        int allocateSize =
                Hash.SIZE_IN_BYTES * 6 + //address,bundle,trunk,branch,obsoleteTag,tag
                        Long.BYTES * 9 + //value,currentIndex,lastIndex,timestamp,attachmentTimestampLowerBound,attachmentTimestampUpperBound,arrivalTime,height
                        Integer.BYTES * 3 + //validity,type,snapshot
                        1 + //solid
                        sender.getBytes().length; //sender
        ByteBuffer buffer = ByteBuffer.allocate(allocateSize);
        buffer.put(address.bytes());
        buffer.put(bundle.bytes());
        buffer.put(trunk.bytes());
        buffer.put(branch.bytes());
        buffer.put(obsoleteTag.bytes());
        buffer.put(Serializer.serialize(value));
        buffer.put(Serializer.serialize(currentIndex));
        buffer.put(Serializer.serialize(lastIndex));
        buffer.put(Serializer.serialize(timestamp));

        buffer.put(tag.bytes());
        buffer.put(Serializer.serialize(attachmentTimestamp));
        buffer.put(Serializer.serialize(attachmentTimestampLowerBound));
        buffer.put(Serializer.serialize(attachmentTimestampUpperBound));

        buffer.put(Serializer.serialize(validity));
        buffer.put(Serializer.serialize(type));
        buffer.put(Serializer.serialize(arrivalTime));
        buffer.put(Serializer.serialize(height));

        // encode booleans in 1 byte
        byte flags = 0;
        flags |= solid ? IS_SOLID_BITMASK : 0;
        flags |= milestone ? IS_MILESTONE_BITMASK : 0;
        buffer.put(flags);

        buffer.put(Serializer.serialize(snapshot));
        buffer.put(sender.getBytes());
        return buffer.array();
    }

    @Override
    public void readMetadata(byte[] bytes) {
        int i = 0;
        if(bytes != null) {
            address = HashFactory.ADDRESS.create(bytes, i, Hash.SIZE_IN_BYTES);
            i += Hash.SIZE_IN_BYTES;
            bundle = HashFactory.BUNDLE.create(bytes, i, Hash.SIZE_IN_BYTES);
            i += Hash.SIZE_IN_BYTES;
            trunk = HashFactory.TRANSACTION.create(bytes, i, Hash.SIZE_IN_BYTES);
            i += Hash.SIZE_IN_BYTES;
            branch = HashFactory.TRANSACTION.create(bytes, i, Hash.SIZE_IN_BYTES);
            i += Hash.SIZE_IN_BYTES;
            obsoleteTag = HashFactory.OBSOLETETAG.create(bytes, i, Hash.SIZE_IN_BYTES);
            i += Hash.SIZE_IN_BYTES;
            value = Serializer.getLong(bytes, i);
            i += Long.BYTES;
            currentIndex = Serializer.getLong(bytes, i);
            i += Long.BYTES;
            lastIndex = Serializer.getLong(bytes, i);
            i += Long.BYTES;
            timestamp = Serializer.getLong(bytes, i);
            i += Long.BYTES;

            tag = HashFactory.TAG.create(bytes, i, Hash.SIZE_IN_BYTES);
            i += Hash.SIZE_IN_BYTES;
            attachmentTimestamp = Serializer.getLong(bytes, i);
            i += Long.BYTES;
            attachmentTimestampLowerBound = Serializer.getLong(bytes, i);
            i += Long.BYTES;
            attachmentTimestampUpperBound = Serializer.getLong(bytes, i);
            i += Long.BYTES;

            validity = Serializer.getInteger(bytes, i);
            i += Integer.BYTES;
            type = Serializer.getInteger(bytes, i);
            i += Integer.BYTES;
            arrivalTime = Serializer.getLong(bytes, i);
            i += Long.BYTES;
            height = Serializer.getLong(bytes, i);
            i += Long.BYTES;

            // decode the boolean byte by checking the bitmasks
            solid = (bytes[i] & IS_SOLID_BITMASK) != 0;
            milestone = (bytes[i] & IS_MILESTONE_BITMASK) != 0;
            i++;

            snapshot = Serializer.getInteger(bytes, i);
            i += Integer.BYTES;
            byte[] senderBytes = new byte[bytes.length - i];
            if (senderBytes.length != 0) {
                System.arraycopy(bytes, i, senderBytes, 0, senderBytes.length);
            }
            sender = new String(senderBytes);
            parsed = true;
        }
    }

    @Override
    public boolean merge() {
        return false;
    }
}
