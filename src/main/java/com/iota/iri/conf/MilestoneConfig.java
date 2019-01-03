package com.iota.iri.conf;

import org.sonatype.inject.Description;

/**
 * Configs that should be used for tracking milestones
 */
public interface MilestoneConfig extends Config {

    /**
     * @return Descriptions#COORDINATOR
     */
    String getCoordinator();

    /**
     * @return {@value Descriptions#DONT_VALIDATE_TESTNET_MILESTONE_SIG}
     */
    boolean isDontValidateTestnetMilestoneSig();

    abstract  class Descriptions {
        private Descriptions(){

        }
        public static final String COORDINATOR = "The address of the coordinator";
        public static final String DONT_VALIDATE_TESTNET_MILESTONE_SIG = "Disable coordinator validation on testnet";
    }
}
