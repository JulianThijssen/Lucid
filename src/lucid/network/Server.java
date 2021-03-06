package lucid.network;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import lucid.Config;
import lucid.exceptions.ChannelReadException;
import lucid.exceptions.ChannelWriteException;
import lucid.exceptions.ConnectionException;
import lucid.exceptions.PacketException;
import lucid.exceptions.ServerStartException;
import lucid.util.Log;
import lucid.util.LogLevel;
import lucid.util.Scheduler;

public abstract class Server implements Runnable {
    /** The TCP channel bound to a port which receives TCP client connection requests. */
    private ServerTcpChannel tcpChannel;

    /** The UDP channel bound to a port which receives UDP client connection requests. */
    private ServerUdpChannel udpChannel;

    /** The Selector which will handle multiple connection requests */
    private Selector selector;

    /** The TCP port of the server, this is the port players must connect through. */
    private int tcpPort = 0;

    /** The UDP port of the server, this is the port players must connect through. */
    private int udpPort = 0;

    /** A list keeping track of all the clients currently connected to the server. */
    private ConnectionMap connectionMap = new ConnectionMap();

    /** List of all the listeners that get notified of connection events */
    private List<ServerListener> listeners = new ArrayList<ServerListener>();

    /** List of all the listeners that get notified of error events */
    private List<ServerErrorListener> errorListeners = new ArrayList<ServerErrorListener>();

    /** Whether the server is running or not */
    private boolean running = false;
    
    
    private class Handshake {
        public long unique;
        public SocketAddress client;
        
        public Handshake(long unique, SocketAddress client) {
            this.unique = unique;
            this.client = client;
        }
    }
    
    private List<Handshake> openHandshakes = new ArrayList<Handshake>();
    private Scheduler sendScheduler = null;

    public Server(int tcpPort, int udpPort) {
        this.tcpPort = tcpPort;
        this.udpPort = udpPort;
        Config.loadConfig();
    }

    /** Initializes the server to be ready for incoming connections. */
    public void start() throws ServerStartException {
        if (running) {
            throw new ServerStartException(ServerError.SERVER_ALREADY_STARTED);
        }
        try {
            selector = Selector.open();

            if (tcpPort != 0) {
                tcpChannel = new ServerTcpChannel(tcpPort, selector);
                tcpChannel.start();
            }

            if (udpPort != 0) {
                udpChannel = new ServerUdpChannel(udpPort, selector);
                udpChannel.start();
            }
            
            Runnable sendTask = new Runnable() {
                @Override
                public void run() {
                    for (Handshake handshake: openHandshakes) {
                        Packet packet = new Packet((short) 0);
                        packet.addLong(handshake.unique);
                        try {
                            udpChannel.sendPacket(packet, handshake.client);
                        } catch (ChannelWriteException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            
            sendScheduler = new Scheduler(sendTask);
            sendScheduler.executeAtFixedRate(500);

            running = true;
            new Thread(this).start();
            notifyServerStart();
        } catch(Exception e) {
            close();
            throw new ServerStartException(ServerError.SERVER_START_FAILURE, e);
        }
    }

    public void run() {
        while(running) {
            listen();
        }
    }

    public boolean isRunning() {
        return running;
    }

    private boolean acceptUdpConnection(long unique, SocketAddress address) {
        try {
            UdpConnection udp = udpChannel.accept(unique, address);

            return connectionMap.addUdpToConnection(udp);
        }
        catch (ConnectionException e) {
            Log.debug(LogLevel.SERVER, "Failed to accept UDP connection: " + e.getMessage());
        }
        return false;
    }

    /** Listens to incoming connection requests from clients and passes them in a Connection class */
    private void listen() {
        try {
            selector.select();
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();

            while (iterator.hasNext()) {
                SelectionKey key = (SelectionKey) iterator.next();

                if (key.isAcceptable()) {
                    accept(key);
                }
                else if (key.isReadable()) {
                    read(key);
                }

                else if (key.isWritable()) {
                    write(key);
                }

                iterator.remove();
            }
        } catch(IOException e) {
            Log.debug(LogLevel.ERROR, "An error occurred while selecting channels");
        }
    }

    private void accept(SelectionKey key) {
        if (connectionMap.size() < Config.MAX_PLAYERS) {
            try {
                TcpConnection tcp = tcpChannel.accept();

                boolean accepted = connectionMap.addTcpToConnection(tcp);

                if (accepted) {
                    notifyConnection(connectionMap.getFromUnique(tcp.getUnique()));
                }
                else {
                    // If the connection already has TCP, deny the new connection
                    Log.debug(LogLevel.SERVER, "Connection denied, client already connected via TCP");
                    key.cancel();
                    tcp.close();
                }
            } catch (ConnectionException e) {
                Log.debug(LogLevel.ERROR, "Failed to accept client: " + e.getMessage());
            }
        }
    }
    
    private boolean containsHandshake(SocketAddress address) {
        for (Handshake handshake: openHandshakes) {
            if (handshake.client == address) {
                return true;
            }
        }
        return false;
    }
    
    private Handshake removeHandshake(SocketAddress address) {
        Iterator<Handshake> it = openHandshakes.iterator();
        
        while (it.hasNext()) {
            Handshake handshake = it.next();
            
            if (handshake.client == address) {
                it.remove();
                return handshake;
            }
        }
        return null;
    }

    private void read(SelectionKey key) throws IOException {
        Object attachment = key.attachment();

        if (attachment instanceof TcpConnection) {
            TcpConnection tcpConnection = (TcpConnection) attachment;
            Connection connection = connectionMap.getFromUnique(tcpConnection.getUnique());

            if (connection == null || tcpConnection == null) {
                return;
            }

            // Attempt to read packets from TCP connection
            try {
                tcpConnection.read();

                while (tcpConnection.hasPackets()) {
                    Packet packet = tcpConnection.readPacket();
                    notifyReceived(connection, packet);
                }
            }
            // The connection to this client broke, disconnect them
            catch (ChannelReadException e) {
                key.cancel();
                tcpConnection.close();
                notifyDisconnection(connection);
                connectionMap.remove(tcpConnection.getUnique());
            }
        }
        else if (attachment instanceof ServerUdpChannel) {
            assert udpChannel.isOpen();

            List<Packet> packets = null;
            // Attempt to read packets from UDP server channel
            try {
                packets = udpChannel.readPacket();
            } catch (ChannelReadException e) {
                // The connection to this client broke, disconnect them
                // TODO Try to recover
                e.printStackTrace();
                return;
            }

            for (Packet packet: packets) {
                SocketAddress address = packet.getSource();

                // FIXME Shouldn't be 0, should be a special SYN packet
                if (packet.getType() == 0) {
                    Log.debug(LogLevel.SERVER, "Received SYN packet");
                    // Attempt to read unique ID from handshake and send acknowledgement
                    try {
                        long unique = packet.getLong();
                        if (!containsHandshake(address)) {
                            openHandshakes.add(new Handshake(unique, address));
                        }
                        
                        Log.debug(LogLevel.SERVER, "Got a new handshake from client with id: " + unique);
                        //sendPacket(handshake, handshake.getSource());
                    }
                    catch (PacketException e) {
                        e.printStackTrace();
                        return;
                    }
                }
                else if (packet.getType() == 1) { // FIXME shouldnt be 0, should be special ACK packet
                    Log.debug(LogLevel.SERVER, "Received ACK packet");
                    // Received acknowledgement from client, complete connection
                    if (containsHandshake(address)) {
                        Handshake handshake = removeHandshake(address);
                        boolean accepted = acceptUdpConnection(handshake.unique, address);
                        
                        if (accepted) {
                            notifyConnection(connectionMap.getFromAddress(packet.getSource()));
                        }
                        else {
                            Log.debug(LogLevel.SERVER, "UDP Connection denied");
                        }
                    }
                }
                else {
                    Connection connection = connectionMap.getFromAddress(packet.getSource());

                    if (connection != null) {
                        notifyReceived(connection, packet);
                    } else {
                        Log.debug(LogLevel.ERROR, "Received a packet from unknown connection");
                    }
                }
            }
        }
        else {
            Log.debug(LogLevel.ERROR, "There is an unknown readable channel " + attachment);
        }
    }

    private void write(SelectionKey key) {
        Object attachment = key.attachment();

        if (attachment instanceof TcpConnection) {
            TcpConnection tcp = (TcpConnection) attachment;

            try {
                tcp.write();
            } catch (ChannelWriteException e) {
                // The connection to this client broke, disconnect them
                tcp.close();
                key.cancel();
                notifyDisconnection(connectionMap.getFromUnique(tcp.getUnique()));
                connectionMap.remove(tcp.getUnique());
            }
        }
        else if (attachment instanceof UdpConnection) {
            UdpConnection udp = (UdpConnection) attachment;

            try {
                udp.write();
            }
            catch (ChannelWriteException e) {
                // The connection to this client broke, disconnect them
                System.out.println("UDP connection to client broke");
                udp.close();
                key.cancel();
                notifyDisconnection(connectionMap.getFromUnique(udp.getUnique()));
                connectionMap.remove(udp.getUnique());
            }
        }
        else {
            Log.debug(LogLevel.ERROR, "There is an unknown writable channel " + attachment);
        }
    }

    /** Broadcast a packet to all clients connected to the server */
    public void broadcastTcp(Packet packet) {
        if (packet == null) { return; }

        for(Connection connection: connectionMap.getConnections()) {
            if(connection != null) {
                connection.sendTcp(packet);
            }
        }
    }

    /** Broadcast a packet to all clients connected to the server */
    public void broadcastUdp(Packet packet) {
        if (packet == null) { return; }

        for(Connection connection: connectionMap.getConnections()) {
            if(connection != null) {
                connection.sendUdp(packet);
            }
        }
    }

    public void addListener(ServerListener listener) {
        listeners.add(listener);
    }

    public void addErrorListener(ServerErrorListener listener) {
        errorListeners.add(listener);
    }

    /** Notify all listeners of the server start */
    private void notifyServerStart() {
        for (ServerListener sl: listeners) {
            sl.onServerStart();
        }
    }

    /** Notify all listeners of the server stop */
    private void notifyServerStop() {
        for (ServerListener sl: listeners) {
            sl.onServerStop();
        }
    }

    /** Notify all listeners of the new connection */
    private void notifyConnection(Connection connection) {
        for (ServerListener sl: listeners) {
            sl.onConnection(connection);
        }
    }

    /** Notify all listeners of the disconnection */
    private void notifyDisconnection(Connection connection) {
        for (ServerListener sl: listeners) {
            sl.onDisconnect(connection);
        }
    }

    /** Notify all listeners of received packets */
    private void notifyReceived(Connection connection, Packet packet) {
        for (ServerListener sl: listeners) {
            sl.onReceived(connection, packet);
        }
    }

    /** Notify all listeners of server error */
    private void notifyError(ServerError error) {
        for (ServerErrorListener sel: errorListeners) {
            sel.onServerError(error);
        }
    }

    /** Closes all connections, and shuts down the server. */
    public void close() {
        running = false;

        for(Connection connection: connectionMap.getConnections()) {
            if(connection != null) {
                connection.close();
            }
        }
        if (tcpChannel != null) {
            tcpChannel.close();
        }
        if (udpChannel != null) {
            udpChannel.close();
        }

        notifyServerStop();
    }
}
