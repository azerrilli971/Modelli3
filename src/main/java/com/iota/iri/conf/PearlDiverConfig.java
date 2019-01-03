package com.iota.iri.conf;

/**
 * Configurations for PearlDiver proof-of-work hasher.
 */
public interface PearlDiverConfig extends Config {

    /**
     * @return {@value PearlDiverConfig.Descriptions#POW_THREADS}
     */
    int getPowThreads();

    /**
    * Field descriptions
    */
    abstract  class Descriptions {
        private Descriptions(){
            
        }
        public static final String POW_THREADS = "Number of threads to use for proof-of-work calculation";
    }
}
