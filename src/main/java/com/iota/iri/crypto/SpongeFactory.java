package com.iota.iri.crypto;

/**
 * Creates sponge objects, based on required {@code Mode}
 *
 * @see Mode
 */
public interface SpongeFactory {
    /**
     * Modes of sponge constructions.
     * Determines which hash function we will use
     */
    public enum Mode {
        CURLP81,
        CURLP27,
        KERL,
        //BCURLT
    }

    /**
     * Creates a new sponge object, based on required {@code Mode}
     * @param mode name of the hash function to use.
     * @return a newly initialized sponge
     */
    public static Sponge create(Mode mode){
        switch (mode) {
            case CURLP81: return new Curl(mode);
            case CURLP27: return new Curl(mode);
            case KERL: return new Kerl();
            default: return null;
        }
    }
}
