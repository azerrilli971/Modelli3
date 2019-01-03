package com.iota.iri.conf;

public interface ZMQConfig extends Config {

    boolean isZmqEnabled();

    int getZmqPort();

    int getZmqThreads();

    String getZmqIpc();

    abstract static class Descriptions {

        private Descriptions(){
            super();
        }

        public static final String ZMQ_ENABLED = "Enabling zmq channels.";
        public static final String ZMQ_PORT = "The port used to connect to the ZMQ feed";
        public static final String ZMQ_IPC = "The path that is used to communicate with ZMQ in IPC";
    }
}
