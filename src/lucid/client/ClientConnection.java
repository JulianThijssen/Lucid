package lucid.client;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import lucid.exceptions.ChannelReadException;
import lucid.exceptions.ChannelWriteException;
import lucid.network.ConnectionStatus;
import lucid.network.NetworkChannel;
import lucid.network.Packet;
import lucid.util.Log;
import lucid.util.LogLevel;
import lucid.util.UniqueGenerator;

public abstract class ClientConnection implements Runnable {
    /** The connection of the client to the server */
    protected NetworkChannel channel;
    
    /** Unique ID of the client */
    protected long unique;
    
    /** The Selector which will listen for reading or writing capabilities */
    protected Selector selector;
    
    /** Status of the client connection */
    private ConnectionStatus status = ConnectionStatus.DISCONNECTED;
    
    /** List of all the listeners that get notified of connection events */
    private List<NetworkListener> listeners = new ArrayList<NetworkListener>();
    
    public ClientConnection() {
        unique = UniqueGenerator.unique;
    }
    
    public void setConnectionStatus(ConnectionStatus status) {
        this.status = status;
    }
    
    public ConnectionStatus getConnectionStatus() {
        return status;
    }
    
    public void addListener(NetworkListener listener) {
        listeners.add(listener);
    }
    
    protected void start() {
        new Thread(this).start();
    }
    
    @Override
    public void run() {
        try {
            while (status != ConnectionStatus.DISCONNECTED) {
                update();

                while (channel.hasPackets()) {
                    Packet packet = channel.receive();
                    
                    onPacketReceived(packet);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    protected void update() throws IOException {        
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
    
    public abstract void onPacketReceived(Packet packet);
    
    public void send(Packet packet) {
        channel.send(packet);
    }

    protected void read() {
        try {
            channel.read();
        }
        catch (ChannelReadException e) {
            Log.debug(LogLevel.ERROR, e.getMessage());
            close();
        }
    }
    
    protected void write() {
        try {
            channel.write();
        }
        catch (ChannelWriteException e) {
            Log.debug(LogLevel.ERROR, e.getMessage());
            close();
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
    
    protected void notifyConnected() {
        status = ConnectionStatus.CONNECTED;
        for(NetworkListener listener: listeners) {
            listener.connected();
        }
    }

    protected void notifyReceived(Packet packet) {
        for(NetworkListener listener: listeners) {
            listener.received(packet);
        }
    }

    protected void notifyDisconnected() {
        status = ConnectionStatus.DISCONNECTED;
        for(NetworkListener listener: listeners) {
            listener.disconnected();
        }
    }
}
