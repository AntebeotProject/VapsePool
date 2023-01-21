package org.antibiotic.pool;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.*;
import java.util.*;

/**
 *  @author oruge + https://gist.github.com/MaZderMind/0be1dea275ea25b39dc753583d742851
 *  @category server
 *  Implementation of AsyncServer for our stratum server
 *  Example code on Kotlin
 *     AsyncServer.DebugEnabled = true
 *     var s = AsyncServer("localhost", 3334)
 *     while(true) {
 *         var updates = s.update()
 *         for (update in updates) {
 *             var last_msg = update.getLast_msg()
 *             update.write("PONG: $last_msg")
 *         }
 *     }
 *     Classes here is final. not need it open.
 */
import java.util.Iterator;


final public class AsyncServer {
    public static boolean DebugEnabled = false;
    final static void WriteDebug(String format, Object ... args) {
        if (DebugEnabled) {
            System.out.print("[DEBUG] [ASYNCSERVER]");
            System.out.printf(format, args);
            System.out.println();
        }

    }
    final public class Client {
        private static final ByteBuffer REUSABLE_BYTE_BUFFER = ByteBuffer.allocate(2056);
        private static final CharBuffer REUSABLE_CHAR_BUFFER = CharBuffer.allocate(2056);
        private final CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
        private final CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
        private final SegmentedBuffer segmentedBuffer = new SegmentedBuffer();
        private SocketChannel m_socket = null; //
        private String last_msg = null;
        private String m_session_id = null;
        private String m_extranonce = "";
        private int m_extranonce2_size = 0;

        public void setSessionId(String session_id) {
            m_session_id = session_id;
        }
        public String getSessionId() {
            return m_session_id;
        }
        public String getLast_msg() {
            return last_msg;
        }
        public boolean equals(Object c) {
            if ( !(c instanceof Client) ) return false;
            if ( c.getClass() != getClass() ) return false;
            Client anotherClient = (Client) c;
            return anotherClient.m_socket.equals(m_socket); // just check them socket
        }
        public Client(SocketChannel c) {
            m_socket = c;
            isActive = true;
        }
        public String read() throws IOException {
            return read(m_socket);
        }
        public void write(String msg) throws IOException {
            write(m_socket, msg);
        }
        /*
        private Long mlastActiveTimeSec = 0L;
        public Long getLastActiveTime() {
            return mlastActiveTimeSec;
        }
        public void updateLastActivetime() {
            mlastActiveTimeSec = System.currentTimeMillis() / 1000;
        }
        */

        private boolean m_authorized = false;
        public void setAuthorize(boolean auth) {
            m_authorized = true;
        }
        public boolean getAuthorize() {
            return m_authorized;
        }
        public boolean isActive = false;
        public boolean getIsActive() {
            return isActive;
        }
        public void closeConnection() {
            try {
                for(var idx = 0; idx < m_clients.size(); idx++) {
                    var c = m_clients.get(idx);
                    if (c.equals(this)) {
                        m_clients.remove(idx);
                        break;
                    }
                }
                isActive = false;
                m_socket.close();
            } catch(IOException e) {
                System.err.println("Socket close error " + e.toString());
            }
        }
        public void write(SocketChannel s, String msg) throws IOException {
            try {
                WriteDebug("~~~" + msg + "~~~");
                s.write(encoder.encode(CharBuffer.wrap(msg + "\n"))); // By UTF-8 with wrap. UTF is optional in our way. maybe delete it. and change to ascii just.
            } catch (ClosedChannelException _e) {
                this.closeConnection();
            }
        }
        public String read(SocketChannel s) throws IOException {
            StringBuilder sb = new StringBuilder();
            REUSABLE_BYTE_BUFFER.clear();
            boolean eof = s.read(REUSABLE_BYTE_BUFFER) == -1;
            REUSABLE_BYTE_BUFFER.flip();
            WriteDebug("read %d bytes to byte-buffer", REUSABLE_BYTE_BUFFER.limit());
            CoderResult decodeResult;
            do {
                REUSABLE_CHAR_BUFFER.clear();
                decodeResult = decoder.decode(REUSABLE_BYTE_BUFFER, REUSABLE_CHAR_BUFFER, false);
                REUSABLE_CHAR_BUFFER.flip();
                WriteDebug("decoded %d chars from byte-buffer", REUSABLE_CHAR_BUFFER.length());
                segmentedBuffer.put(REUSABLE_CHAR_BUFFER);
            } while (decodeResult == CoderResult.OVERFLOW); // read message
            while (segmentedBuffer.hasNext()) {
                String data = segmentedBuffer.next(); //.trim();
                sb.append(data);
                // throw new ClosedChannelException(); if you want to drop connection
            }
            if (eof) {
                throw new ClosedChannelException();
            }
            WriteDebug("Last Message %s", sb.toString());
            last_msg = sb.toString();
            return sb.toString();
        }
        /*
            from docs
            Extranonce1 - Hex-encoded, per-connection unique string which will be used for coinbase serialization later. Keep it safe!
            ok. we keep it.
         */
        public void setExtranonce2Size(int extranonce) {
            this.m_extranonce2_size = extranonce;
        }
        public void setExtranonce(String ex) {
            this.m_extranonce = ex;
        }
        public String getExtranonce() {
            return m_extranonce;
        }
        public int getExtranonce2Size() {
            return m_extranonce2_size;
        }
    }

    private Selector mSelector = null;
    private ServerSocketChannel mServerSocketChannel = null;
    final private String defHost = "localhost";
    final private int defPort = 3333;
    private void BindServer(String bind, int port) throws IOException {
        if (mSelector != null || mServerSocketChannel != null) {
            throw new IOException("Server bind already."); // can
        }
        mSelector = Selector.open();
        mServerSocketChannel = ServerSocketChannel.open();
        mServerSocketChannel.bind(new InetSocketAddress(bind, port));
        mServerSocketChannel.configureBlocking(false);
        mServerSocketChannel.register(mSelector, SelectionKey.OP_ACCEPT);
    }
    public AsyncServer(int port) throws IOException {
        BindServer(defHost, port);
    }
    public AsyncServer(String host) throws IOException {
        BindServer(host, defPort);
    }
    public AsyncServer(String host, int port) throws IOException {
        BindServer(host, port);
    }
    public AsyncServer() throws IOException {
        BindServer(defHost, defPort);
    }

    /*HashSet*/ private ArrayList<Client> m_clients = new ArrayList<Client>();

    public final ArrayList<Client> update() throws IOException {
        mSelector.select();
        ArrayList<Client> wClient = new ArrayList<Client>(); // clients that want to get response of server

        Set<SelectionKey> selectedKeys = mSelector.selectedKeys();
        Iterator<SelectionKey> iter = selectedKeys.iterator();
        while (iter.hasNext()) {
            SelectionKey key = iter.next();

            if (key.isAcceptable()) {
                SocketChannel client = mServerSocketChannel.accept();
                WriteDebug("Incoming Connection from %s", client.getRemoteAddress());
                client.configureBlocking(false);
                SelectionKey newKey = client.register(mSelector, SelectionKey.OP_READ);
                var c = new Client(client);
                m_clients.add(c);
                newKey.attach(c); // read from client
            }
            if (key.isReadable()) {
                SocketChannel client = (SocketChannel) key.channel();
                Client ourAttachmentClient = (Client) key.attachment();
                try {
                    ourAttachmentClient.read(client);
                    wClient.add(ourAttachmentClient);
                } catch (Exception e) {
                    WriteDebug("Connection from %s closed", client.getRemoteAddress());
                    key.cancel();
                    ourAttachmentClient.closeConnection(); // attachment is our object of class of our AsynServer.Client
                }
            }

            iter.remove();
        }
        return wClient;
    }
}

final class SegmentedBuffer implements Iterator<String> {
    private final String terminator;
    private String buffer = "";
    private boolean isFlushing = false;

    public SegmentedBuffer() {
        this("\n");
    }

    public SegmentedBuffer(String terminator) {
        this.terminator = terminator;
    }

    public void put(CharSequence charSequence) {
        buffer += charSequence;
    }

    public void flush() {
        isFlushing = buffer.length() > 0;
    }

    @Override
    public boolean hasNext() {
        return isFlushing || buffer.contains(terminator);
    }

    @Override
    public String next() {
        if (isFlushing) {
            isFlushing = false;
            String line = buffer;
            buffer = "";
            return line;
        }

        int index = buffer.indexOf(terminator);
        String line = buffer.substring(0, index);
        buffer = buffer.substring(index + 1);
        return line;
    }
}