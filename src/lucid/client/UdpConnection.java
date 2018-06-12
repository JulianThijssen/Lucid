package lucid.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import lucid.exceptions.ChannelReadException;
import lucid.exceptions.ChannelWriteException;
import lucid.network.ConnectionStatus;
import lucid.network.Packet;
import lucid.network.UdpChannel;
import lucid.util.Log;
import lucid.util.LogLevel;
import lucid.util.Scheduler;
import lucid.util.UniqueGenerator;

public class UdpConnection implements Runnable {
    private class Handshake {
        private final Packet SYN;
        private final Packet ACK;
        
        private Scheduler retryScheduler = null;
        
        public Handshake(long unique) {
            SYN = new Packet((short) 0);
            SYN.addLong(unique);
            ACK = new Packet((short) 1);
            
            Runnable retryTask = new Runnable() {
                @Override
                public void run() {
                    sendSyn();
                }
            };
            retryScheduler = new Scheduler(retryTask);
        }
        
        public void init() {
            retryScheduler.executeAtFixedRate(500);
        }
        
        public void complete() {
            retryScheduler.stop();
            // Acknowledge SYN-ACK
            sendAck();
        }
        
        private void sendSyn() {
            send(SYN);
        }
        
        public void sendAck() {
            send(ACK);
        }
    }
    
    /** The connection of the client to the server */
    private UdpChannel channel;

    private InetSocketAddress address = null;

    /** Unique ID of the client */
    private long unique;
    
    /** The Selector which will listen for reading or writing capabilities */
    private Selector selector;

    /** Status of the client connection */
    private ConnectionStatus status = ConnectionStatus.DISCONNECTED;
    
    /** List of all the listeners that get notified of connection events */
    private List<NetworkListener> listeners = new ArrayList<NetworkListener>();
    
    private Handshake handshake;
    
    public UdpConnection() {
        unique = UniqueGenerator.unique;
        
        handshake = new Handshake(unique);
    }
    
    public boolean connect(String host, int port) {
        address = new InetSocketAddress(host, port);

        try {
            DatagramChannel datagramChannel = DatagramChannel.open();
            datagramChannel.connect(address);
            datagramChannel.configureBlocking(false);

            channel = new UdpChannel(datagramChannel);
            
            selector = Selector.open();
            SelectionKey key = datagramChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            key.attach(this);

            status = ConnectionStatus.CONNECTING;
            new Thread(this).start();
            
            // Initiate the handshake
            handshake.init();

        } catch(IOException e) {
            e.printStackTrace();
            close();
            Log.debug(LogLevel.CLIENT, String.format("Failed to connect to host: %s at port: %d", host, port));
            return false;
        }

        return true;
    }

    public ConnectionStatus getConnectionStatus() {
        return status;
    }

    @Override
    public void run() {
        try {
            while (status != ConnectionStatus.DISCONNECTED) {
                // Read or write packets
                update();
                
                while (channel.hasPackets()) {
                    Packet packet = channel.receive();
                    
                    if (packet.getType() == 0) {
                        if (status == ConnectionStatus.CONNECTING) {
                            Log.debug(LogLevel.CLIENT, "Received SYN-ACK packet");
                            handshake.complete();
                            notifyConnected();
                            Log.debug(LogLevel.CLIENT, String.format("Successfully connected to host: " + address));
                        }
                        else if (status == ConnectionStatus.CONNECTED) {
                            // Server didn't receive ACK packet, so re-send it
                            handshake.complete();
                        }
                    }
                    else {
                        notifyReceived(packet);
                    }
                }
            }
        } catch (IOException e) {
            close();
        }
    }

    private void update() throws IOException {
        if (!channel.isConnected()) {
            close();
            return;
        }
        
        selector.select();
        Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
        
        while (iterator.hasNext()) {
            SelectionKey key = (SelectionKey) iterator.next();

            if (key.isReadable()) {
                read();
            }

            else if (key.isWritable()) {
                write();
            }

            iterator.remove();
        }
    }

    public void send(Packet packet) {
        channel.send(packet);
    }

    private void read() {
        try {
            channel.read();
        }
        catch (ChannelReadException e) {
            Log.debug(LogLevel.ERROR, e.getMessage());
            close();
        }
    }
    
    private void write() {
        try {
            channel.write();
        }
        catch (ChannelWriteException e) {
            Log.debug(LogLevel.ERROR, e.getMessage());
            close();
        }
    }

    public void addListener(NetworkListener listener) {
        listeners.add(listener);
    }

    private void notifyConnected() {
        status = ConnectionStatus.CONNECTED;
        for(NetworkListener listener: listeners) {
            listener.connected();
        }
    }

    private void notifyReceived(Packet packet) {
        for(NetworkListener listener: listeners) {
            listener.received(packet);
        }
    }

    private void notifyDisconnected() {
        status = ConnectionStatus.DISCONNECTED;
        for(NetworkListener listener: listeners) {
            listener.disconnected();
        }
    }

    public void close() {
        try {
            notifyDisconnected();
            if (channel != null) { channel.close(); }
        } catch(IOException e) {
            //TODO Log.debug("Connection to the server was closed");
        }
        Log.debug(LogLevel.CLIENT, "Connection closed");
    }
}
