package com.iota.iri.conf;

/**
 * Configurations for IXI modules
 */
public interface IXIConfig extends Config {

    String IXI_DIR = "ixi";

    /**
     * @return Descriptions#IXI_DIR
     */
    String getIxiDir();

    abstract class Descriptions {
        private Descriptions() {
            throw new IllegalStateException("Utility class");
        }

        public static final String IXI_DIR = "The folder where ixi modules should be added for automatic discovery by IRI.";
    }
}
