package com.iota.iri.conf;

/**
 *  General configuration parameters that every module in IRI needs.
 */
public interface Config  {

    String TESTNET_FLAG = "--testnet";

    /**
     * @return {@value Descriptions#TESTNET}
     */
    boolean isTestnet();

    abstract static class Descriptions {
        private Descriptions() {

        }
        public static final String TESTNET = "Start in testnet mode.";
    }

     class DescriptionHelper {

         private DescriptionHelper() {
             throw new IllegalStateException("Conf class");
         }
         private static final String PROB_OF = "A number between 0 and 1 that represents the probability of ";

         public static String getProbOf() {
             return PROB_OF;
         }
     }
}
