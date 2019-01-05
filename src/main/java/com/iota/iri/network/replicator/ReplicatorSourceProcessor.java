package com.iota.iri.network.replicator;

import com.iota.iri.conf.MainnetConfig;
import com.iota.iri.conf.TestnetConfig;
import com.iota.iri.network.Neighbor;
import com.iota.iri.network.Node;
import com.iota.iri.network.TCPNeighbor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.List;
import java.util.zip.CRC32;

class ReplicatorSourceProcessor implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ReplicatorSourceProcessor.class);

    private final Socket connection;

    private static final boolean SHUTDOWN = false;
    private final Node node;
    private final int maxPeers;
    private final boolean testnet;
    private final ReplicatorSinkPool replicatorSinkPool;
    private final int packetSize;

    private boolean existingNeighbor;
    
    private TCPNeighbor neighbor;

    public ReplicatorSourceProcessor(final ReplicatorSinkPool replicatorSinkPool,
                                     final Socket connection,
                                     final Node node,
                                     final int maxPeers,
                                     final boolean testnet) {
        this.connection = connection;
        this.node = node;
        this.maxPeers = maxPeers;
        this.testnet = testnet;
        this.replicatorSinkPool = replicatorSinkPool;
        this.packetSize = testnet
                ? TestnetConfig.Defaults.PACKET_SIZE
                : MainnetConfig.Defaults.PACKET_SIZE;
    }

    @Override
    public void run() {
        int count;
        byte[] data = new byte[2000];
        int offset = 0;
        boolean finallyClose = true;

        try {
            SocketAddress address = connection.getRemoteSocketAddress();
            InetSocketAddress inetSocketAddress = (InetSocketAddress) address;

            fistSock(inetSocketAddress);

            if (existNeigh(inetSocketAddress)) {
                return;
            }

            if ( neighbor.getSource() != null ) {
                log.info("Source {} already connected", inetSocketAddress.getAddress().getHostAddress());
                finallyClose = false;
                return;
            }
            neighbor.setSource(connection);
            
            // Read neighbors tcp listener port number.
            InputStream stream = connection.getInputStream();
            offset = 0;
            count = getCount(data, offset, stream, ReplicatorSinkPool.PORT_BYTES - offset, ReplicatorSinkPool.PORT_BYTES);

            if (connectionState(count)) {
                return;
            }

            arrayCopy(data);

            createSink();

            connetionEstablished(inetSocketAddress);

            connection.setSoTimeout(0);  // infinite timeout - blocking read

            offset = 0;
            while (!SHUTDOWN && !neighbor.isStopped()) {

                count = getCount(data, offset, stream, packetSize - offset + ReplicatorSinkProcessor.CRC32_BYTES, packetSize + ReplicatorSinkProcessor.CRC32_BYTES);

                if (isaBoolean(count == -1, connection.isClosed())) {
                    break;
                }
                
                offset = 0;

                tryblock(data, address, inetSocketAddress);
            }
        } catch (IOException e) {
            catchMe(e);
        } finally {
            finalClause(finallyClose);
        }
    }

    private void tryblock(byte[] data, SocketAddress address, InetSocketAddress inetSocketAddress) {
        try {
            tryMet(data, address);
        }
          catch (IllegalStateException e) {
            log.error("Queue is full for neighbor IP {}",inetSocketAddress.getAddress().getHostAddress());
        } catch (final RuntimeException e) {
            log.error("Transaction processing runtime exception ",e);
            neighbor.incInvalidTransactions();
        } catch (Exception e) {
            transactionLog(e);
        }
    }

    private boolean existNeigh(InetSocketAddress inetSocketAddress) throws IOException {
        if (!existingNeighbor) {
            int maxPeersAllowed = maxPeers;
            if (isaBoolean(!testnet, Neighbor.getNumPeers() >= maxPeersAllowed)) {
                connects(inetSocketAddress, maxPeersAllowed);
                return true;
            } else {
                getNodes(inetSocketAddress);
            }
        }
        return false;
    }

    private void connects(InetSocketAddress inetSocketAddress, int maxPeersAllowed) throws IOException {
        String hostAndPort = inetSocketAddress.getHostName() + ":" + (inetSocketAddress.getPort());
        nestedHost(inetSocketAddress, maxPeersAllowed, hostAndPort);
        connection.getInputStream().close();
        connection.shutdownInput();
        connection.shutdownOutput();
        connection.close();
    }

    private void transactionLog(Exception e) {
        if (log.isInfoEnabled()) {
            log.info(String.format("Transaction processing exception %s ", e.getMessage()));
        }
        log.error("Transaction processing exception ",e);
    }

    private void catchMe(IOException e) {
        log.error("***** NETWORK ALERT ***** TCP connection reset by neighbor {}, source closed, {}", neighbor.getHostAddress(), e.getMessage());
        replicatorSinkPool.shutdownSink(neighbor);
    }

    private void arrayCopy(byte[] data) {
        byte [] pbytes = new byte [10];
        System.arraycopy(data, 0, pbytes, 0, ReplicatorSinkPool.PORT_BYTES);
        neighbor.setTcpPort((int)Long.parseLong(new String(pbytes)));
    }

    private void tryMet(byte[] data, SocketAddress address) {
        CRC32 crc32 = getCrc32(data);
        String crc32String = getString(crc32);
        byte [] crc32Bytes = crc32String.getBytes();

        boolean crcError = false;
        crcError = isCrcError(data, crc32Bytes, crcError);
        if (!crcError) {
            node.preProcessReceivedData(data, address, "tcp");
        }
    }

    private void connetionEstablished(InetSocketAddress inetSocketAddress) {
        if (connection.isConnected()) {
            log.info("----- NETWORK INFO ----- Source {} is connected", inetSocketAddress.getAddress().getHostAddress());
        }
    }

    private void createSink() {
        if (neighbor.getSink() == null) {
            log.info("Creating sink for {}", neighbor.getHostAddress());
            replicatorSinkPool.createSink(neighbor);
        }
    }

    private boolean connectionState(int count) {
        if (isaBoolean(count == -1, connection.isClosed())) {
            log.error("Did not receive neighbors listener port");
            return true;
        }
        return false;
    }

    private void getNodes(InetSocketAddress inetSocketAddress) {
        final TCPNeighbor freshNeighbor = new TCPNeighbor(inetSocketAddress, false);
        node.getNeighbors().add(freshNeighbor);
        neighbor = freshNeighbor;
        Neighbor.incNumPeers();
    }

    private boolean isaBoolean(boolean b, boolean b2) {
        return b || b2;
    }

    private void nestedHost(InetSocketAddress inetSocketAddress, int maxPeersAllowed, String hostAndPort) {
        if (Node.getRejectedAddresses().add(inetSocketAddress.getHostName())) {
            String sb = "***** NETWORK ALERT ***** Got connected from unknown neighbor tcp://"
                + hostAndPort
                + " (" + inetSocketAddress.getAddress().getHostAddress() + ") - closing connection";
            if (testnet && Neighbor.getNumPeers() >= maxPeersAllowed) {
                sb = sb + (" (max-peers allowed is "+ (maxPeersAllowed)+")");
            }
            log.info(sb);
        }
    }

    private void fistSock(InetSocketAddress inetSocketAddress) {
        existingNeighbor = false;
        List<Neighbor> neighbors = node.getNeighbors();
        neighbors.stream().filter(n -> n instanceof TCPNeighbor)
                .map(n -> ((TCPNeighbor) n))
                .forEach(n -> {
                    String hisAddress = inetSocketAddress.getAddress().getHostAddress();
                    if (n.getHostAddress().equals(hisAddress)) {
                        existingNeighbor = true;
                        neighbor = n;
                    }
                });
    }

    private int getCount(byte[] data, int offset, InputStream stream, int i, int i2) throws IOException {
        int count;
        while (((count = stream.read(data, offset, (i))) != -1)
                && (offset < (i2))) {
            offset += count;
        }
        return count;
    }

    private CRC32 getCrc32(byte[] data) {
        CRC32 crc32 = new CRC32();
        for (int i=0; i<packetSize; i++) {
            crc32.update(data[i]);
        }
        return crc32;
    }

    private String getString(CRC32 crc32) {
        String crc32String = Long.toHexString(crc32.getValue());
        while (crc32String.length() < ReplicatorSinkProcessor.CRC32_BYTES) {
            crc32String = String.format("0%s", crc32String);
        }
        return crc32String;
    }

    private void finalClause(boolean finallyClose) {
        if (neighbor != null && finallyClose) {
            replicatorSinkPool.shutdownSink(neighbor);
            neighbor.setSource(null);
            neighbor.setSink(null);

        }
    }

    private boolean isCrcError(byte[] data, byte[] crc32Bytes, boolean crcError) {
        for (int i = 0; i< ReplicatorSinkProcessor.CRC32_BYTES; i++) {
            if (crc32Bytes[i] != data[packetSize + i]) {
                crcError = true;
                break;
            }
        }
        return crcError;
    }
}
