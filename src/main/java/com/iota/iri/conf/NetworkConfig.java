package com.iota.iri.conf;

import java.util.List;

/**
 * Configurations for the node networking. Including ports, DNS settings, list of neighbors,
 * and various optimization parameters.
 */
public interface NetworkConfig extends Config {

    /**
     * @return Descriptions#UDP_RECEIVER_PORT
     */
    int getUdpReceiverPort();

    /**
     * @return Descriptions#TCP_RECEIVER_PORT
     */
    int getTcpReceiverPort();

    /**
     * @return Descriptions#P_REMOVE_REQUEST
     */
    double getpRemoveRequest();

    /**
     * @return Descriptions#SEND_LIMIT
     */
    int getSendLimit();

    /**
     * @return Descriptions#MAX_PEERS
     */
    int getMaxPeers();

    /**
     * @return Descriptions#DNS_REFRESHER_ENABLED
     */
    boolean isDnsRefresherEnabled();

    /**
     * @return Descriptions#DNS_RESOLUTION_ENABLED
     */
    boolean isDnsResolutionEnabled();

    /**
     * @return Descriptions#NEIGHBORS
     */
    List<String> getNeighbors();

    /**
     * @return Descriptions#Q_SIZE_NODE
     */
    int getqSizeNode();

    /**
     * @return Descriptions#P_DROP_CACHE_ENTRY
     */
    double getpDropCacheEntry();

    /**
     * @return Descriptions#CACHE_SIZE_BYTES
     */
    int getCacheSizeBytes();

    abstract  class Descriptions {
        private Descriptions() {

        }
        public static final String PROB2 = DescriptionHelper.getProbOf();
        public static final String UDP_RECEIVER_PORT = "The UDP Receiver Port.";
        public static final String TCP_RECEIVER_PORT = "The TCP Receiver Port.";
        public static final String P_REMOVE_REQUEST = "A number between 0 and 1 that represents the probability of  stopping to request a transaction. This number should be " +
            "closer to 0 so non-existing transaction hashes will eventually be removed.";
        public static final String SEND_LIMIT = "The maximum number of packets that may be sent by this node in a 1 second interval. If this number is below 0 then there is no limit.";
        public static final String MAX_PEERS = "The maximum number of non mutually tethered connections allowed. Works only in testnet mode";
        public static final String DNS_REFRESHER_ENABLED = "Reconnect to neighbors that have dynamic IPs.";
        public static final String DNS_RESOLUTION_ENABLED = "Enable using DNS for neighbor peering.";
        public static final String NEIGHBORS = "Urls of peer iota nodes.";
        public static final String Q_SIZE_NODE = "The size of the REPLY, BROADCAST, and RECEIVE network queues.";
        public static final String P_DROP_CACHE_ENTRY =   "A number between 0 and 1 that represents the probability of  dropping recently seen transactions out of the network cache.";
        public static final String CACHE_SIZE_BYTES = "The size of the network cache in bytes";
    }
}
