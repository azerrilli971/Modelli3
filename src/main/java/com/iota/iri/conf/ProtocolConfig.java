package com.iota.iri.conf;

/**
 * Configuration for protocol rules. Controls what transactions will be accepted by the network, and how they will
 * be propagated to other nodes.
 **/
public interface ProtocolConfig extends Config {

    /**
     * @return Descriptions#MWM
     */
    int getMwm();

    /**
     * @return Descriptions#TRANSACTION_PACKET_SIZE
     */
    int getTransactionPacketSize();

    /**
     * @return Descriptions#REQUEST_HASH_SIZE
     */
    int getRequestHashSize();

    /**
     * @return Descriptions#P_REPLY_RANDOM_TIP
     */
    double getpReplyRandomTip();

    double getpDropTransaction();

    /**
     * @return Descriptions#P_SELECT_MILESTONE
     */
    double getpSelectMilestoneChild();

    double getpSendMilestone();

    double getpPropagateRequest();

    interface Descriptions {
        String PROB3 = DescriptionHelper.getProbOf();
        String MWM = "The minimum weight magnitude is the number of trailing 0s that must appear in the end of a transaction hash. Increasing this number by 1 will result in proof of work that is 3 times as hard.";
        String TRANSACTION_PACKET_SIZE = "The size of the packet in bytes received by a node. In the mainnet the packet size should always be 1650. It consists of 1604 bytes of a received transaction and 46 bytes of a requested transaction hash. This value can be changed in order to create testnets with different rules.";
        String REQUEST_HASH_SIZE = "The size of the requested hash in a packet. Its size is derived from the minimal MWM value the network accepts. The larger the MWM -> the more trailing zeroes we can ignore -> smaller hash size.";
        String P_DROP_TRANSACTION = " A number between 0 and 1 that represents the probability of dropping a received transaction. This is used only for testing purposes.";
        String P_SELECT_MILESTONE =  " A number between 0 and 1 that represents the probability of requesting a milestone transaction from a neighbor. This should be a large since it is imperative that we find milestones to get transactions confirmed";
        String P_SEND_MILESTONE = " A number between 0 and 1 that represents the probability of sending a milestone transaction when the node looks for a random transaction to send to a neighbor.";
        String P_REPLY_RANDOM_TIP = "A number between 0 and 1 that represents the probability of replying to a random transaction request, even though your node doesn't have anything to request.";
        String P_PROPAGATE_REQUEST =  "A number between 0 and 1 that represents the probability of  propagating the request of a transaction to a neighbor node if it can't be found. This should be low since we don't want to propagate non-existing transactions that spam the network.";
    }
}
