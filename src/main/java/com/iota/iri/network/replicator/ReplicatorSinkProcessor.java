
package com.iota.iri.network.replicator;

import com.iota.iri.network.TCPNeighbor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;

class ReplicatorSinkProcessor implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ReplicatorSinkProcessor.class);

    private final TCPNeighbor neighbor;

    public final static int CRC32_BYTES = 16;
    private final ReplicatorSinkPool replicatorSinkPool;
    private final int port;
    private int transactionPacketSize;

    public ReplicatorSinkProcessor(final TCPNeighbor neighbor,
                                   final ReplicatorSinkPool replicatorSinkPool,
                                   final int port, int transactionPacketSize) {
        this.neighbor = neighbor;
        this.replicatorSinkPool = replicatorSinkPool;
        this.port = port;
        this.transactionPacketSize = transactionPacketSize;
    }

    @Override
    public void run()  {
        first();
        String remoteAddress = adress();

        try {
            Socket sockets = null;
            sockets = sync(remoteAddress, sockets);

            if (sockets != null) {
                connect(remoteAddress, sockets);
                if (isaBoolean(sockets)) {
                    OutputStream out = getOutputStreams(remoteAddress, sockets);

                    whiloop(remoteAddress, out);
                }
            }
        } catch (Exception e) {
            exceptions(remoteAddress, e);
        }

    }

    private String adress() {
        return neighbor.getHostAddress();
    }

    public class Myexception extends  Exception{

        public Myexception(String message){
            super(message);
        }
    }


    private Socket sync(String remoteAddress, Socket socket){
        synchronized (neighbor) {
            Socket sink = neighbor.getSink();
            if ( sink == null ) {
                try {
                    socket = getSocket(remoteAddress);
                } catch (IOException e) {
                    log.info("got you" ,e);
                }
            }
            else {
                // Sink already created
                endThread(remoteAddress, "Sink {} already created");
            }
        }
        return socket;
    }

    private boolean whiloop(String remoteAddress, OutputStream out) throws InterruptedException, IOException {
        while (ison(replicatorSinkPool.getShutdown(), !neighbor.isStopped())) {
            ByteBuffer message = neighbor.getNextMessage();
            if (neighbor.getSink() != null) {
                if (isaBooleand(neighbor.getSink())) {
                    endThread(remoteAddress, "----- NETWORK INFO ----- Sink {} got disconnected");
                } else {
                    Bo bo = new Bo(remoteAddress, out, message).invoke();
                    if (bo.is()) {
                        return true;
                    }
                    out = bo.getOut();
                }
            }
        }
        return false;
    }

    private void exceptions(String remoteAddress, Exception e) {
        String reason = e.getMessage();
        reason = getStringReason(reason);
        log.error("***** NETWORK ALERT ***** No sink to apiHost {}:{}, reason: {}", remoteAddress, neighbor.getPort(),
                reason);
        test();
    }

    private void test() {
        synchronized (neighbor) {
            Socket sourceSocket = neighbor.getSource();
            intest(sourceSocket);
            neighbor.setSink(null);
        }
    }

    private void intest(Socket sourceSocket) {
        if (sourceSocket != null && (isaBooleand(sourceSocket))) {
            neighbor.setSource(null);
        }
    }

    private String getStringReason(String reason) {
        reason = inget(reason);
        return reason;
    }

    private String inget(String reason) {
        if (reason == null || reason.equals("null")) {
            reason = "closed";
        }
        return reason;
    }

    private OutputStream getOutputStreams(String remoteAddress, Socket socket) throws IOException {
        OutputStream out = getOutputStream(remoteAddress, socket);
        port(out);
        return out;
    }

    private void first() {
        try {
            Thread.sleep(1000);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.info("Interrupted");
        }
    }

    private void connect(String remoteAddress, Socket socket) throws IOException {
        log.info("Connecting sink {}", remoteAddress);
        socket.connect(new InetSocketAddress(remoteAddress, neighbor.getPort()), 30000);
    }

    private OutputStream getOutputStream(String remoteAddress, Socket socket) throws IOException {
        OutputStream out = socket.getOutputStream();
        log.info("----- NETWORK INFO ----- Sink {} is connected", remoteAddress);
        return out;
    }

    private void port(OutputStream out) throws IOException {
        String fmt = "%0"+ ReplicatorSinkPool.PORT_BYTES +"d";
        byte [] portAsByteArray = new byte [10];
        System.arraycopy(String.format(fmt, port).getBytes(), 0,
                portAsByteArray, 0, ReplicatorSinkPool.PORT_BYTES);
        out.write(portAsByteArray);
    }

    private boolean isaBooleand(Socket sink) {
        return sink.isClosed() || !sink.isConnected();
    }


    private boolean ison(boolean shutdown, boolean b) {
        return !shutdown && b;
    }

    private Socket getSocket(String remoteAddress) throws IOException {

        Socket socketd;
        try (Closeable socket = socketd = new Socket()) {
            ((Socket) socket).isConnected();
        }

        log.info("Opening sink {}", remoteAddress);
        try (AutoCloseable ignored = socketd = new Socket()){
            socketd.close();
        } catch (Exception e) {
            log.info("got you" ,e);
        }
        socketd.setSoLinger(true, 0);
        socketd.setSoTimeout(30000);
        neighbor.setSink(socketd);
        return socketd;
    }

    private void endThread(String remoteAddress, String s) {
        log.info(s,
                remoteAddress);
    }

    private boolean isaBoolean(Socket sink) {
        return ison(sink.isClosed(), sink.isConnected());
    }



    private class Mira {
        private boolean myResult;
        private String remoteAddress;
        private OutputStream out;
        private ByteBuffer message;
        private byte[] bytes;


        private String getString(String crc32String) {
            while (crc32String.length() < CRC32_BYTES) {
                crc32String = "0"+crc32String;
            }
            return crc32String;
        }

        public Mira(String remoteAddress, OutputStream out, ByteBuffer message, byte... bytes) {
            this.remoteAddress = remoteAddress;
            this.out = out;
            this.message = message;
            this.bytes = bytes;
        }

        boolean is() {
            return myResult;
        }

        public OutputStream getOut() {
            return out;
        }

        public Mira invoke() throws IOException {
            if (bytes.length == transactionPacketSize) {
                try {
                    test(out, message);
                } catch (IOException e2) {
                    if (isaBoolean(neighbor.getSink())) {
                        out = getOutputStream(out);
                    } else {
                        endThread(remoteAddress, "----- NETWORK INFO ----- Sink {} thread terminating");
                        myResult = true;
                        return this;
                    }
                }
            }
            myResult = false;
            return this;
        }

        private void test(OutputStream out, ByteBuffer message) throws IOException {
            CRC32 crc32 = new CRC32();
            crc32.update(message.array());
            String crc32String = Long.toHexString(crc32.getValue());
            crc32String = getString(crc32String);
            out.write(message.array());
            out.write(crc32String.getBytes());
            out.flush();
            neighbor.incSentTransactions();
        }

        private OutputStream getOutputStream(OutputStream out) throws IOException {
            out.close();
            out = neighbor.getSink().getOutputStream();
            return out;
        }
    }

    private class Bo {
        private boolean myResult;
        private String remoteAddress;
        private OutputStream out;
        private ByteBuffer message;

        public Bo(String remoteAddress, OutputStream out, ByteBuffer message) {
            this.remoteAddress = remoteAddress;
            this.out = out;
            this.message = message;
        }

        boolean is() {
            return myResult;
        }

        public OutputStream getOut() {
            return out;
        }

        public Bo invoke() throws IOException {
            if (isaBooleans(message)) {

                byte[] bytes = message.array();

                Mira mira = new Mira(remoteAddress, out, message, bytes).invoke();
                if (mira.is()) {
                    myResult = true;
                    return this;
                }
                out = mira.getOut();
            }
            myResult = false;
            return this;
        }

        private boolean isaBooleans(ByteBuffer message) {
            return (message != null) && (neighbor.getSink() != null && neighbor.getSink().isConnected())
                    && (neighbor.getSource() != null && neighbor.getSource().isConnected());
        }
    }
}
