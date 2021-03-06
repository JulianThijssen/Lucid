package lucid.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.List;

import lucid.exceptions.ChannelReadException;
import lucid.exceptions.ChannelWriteException;
import lucid.exceptions.ConnectionException;
import lucid.exceptions.ServerStartException;
import lucid.util.Log;
import lucid.util.LogLevel;

public class ServerUdpChannel {
    /** The channel this server will listen on for UDP connections */
    private DatagramChannel channel;
    
    /** The port this server will listen on for UDP connections */
    private int port;
    
    /** The address to which datagram sockets will be bound */
    private SocketAddress hostAddress;

    /** The selector which will select when this channel is ready accept */
    private Selector selector;

    // FIXME Use a constant
    /** Byte buffer used for receiving datagram packets */
    private ByteBuffer in = ByteBuffer.allocate(65535);
    
    /** Keeps track of whether the channel is open and ready */
    private boolean isOpen = false;
    
    public ServerUdpChannel(int port, Selector selector) {
        this.port = port;
        this.selector = selector;
    }

    public final boolean isOpen() {
        return isOpen;
    }

    /** This method will open the channel and attach the channel to the given selector */
    public void start() throws ServerStartException, Exception {
        try {
            hostAddress = new InetSocketAddress(port);
        }
        catch (IllegalArgumentException e) {
            throw new ServerStartException(ServerError.PORT_OUT_OF_RANGE);
        }

        channel = DatagramChannel.open();
        channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        channel.socket().bind(hostAddress);
        channel.configureBlocking(false);

        SelectionKey key = channel.register(selector, SelectionKey.OP_READ);
        key.attach(this);

        isOpen = true;
    }

    /**
     * This method will accept a new connection if one is available
     *
     *  @return  returns the new connection or null if no connection could be accepted
     */
    public UdpConnection accept(long unique, SocketAddress address) throws ConnectionException {
        try {
            // Open a new datagram channel and configure it
            DatagramChannel clientChannel = DatagramChannel.open();
            clientChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            clientChannel.socket().bind(hostAddress);
            clientChannel.configureBlocking(false);

            // Create UDP connection and set received unique ID
            UdpConnection connection = new UdpConnection(clientChannel, address);
            connection.setUnique(unique);

            // Connect UDP connection to client address to limit writing and reading
            connection.connect();

            // Register UDP channel to selector WRITE key
            SelectionKey key = clientChannel.register(selector, SelectionKey.OP_WRITE);
            key.attach(connection);

            Log.debug(LogLevel.SERVER, "Succesfully set up UDP connection to client: " + connection.getAddress());

            return connection;
        } catch(IOException e) {
            throw new ConnectionException(e);
        }
    }

    public List<Packet> readPacket() throws ChannelReadException {
        try {
            // Receive datagram from channel. Could be from unconnected clients.
            in.clear();
            SocketAddress clientAddress = channel.receive(in);
            in.flip();

            // TODO Need to store packet somewhere to receive it
            List<Packet> packets = new ArrayList<Packet>();
            while (in.remaining() > Packet.HEADER_SIZE) {
                Packet packet = Packet.fromByteBuffer(in);
                if (packet == null) {
                    break;
                }
                packet.setSource(clientAddress);
                
                packets.add(packet);
            }

            return packets;
        } catch (IOException e) {
            throw new ChannelReadException("Failed to get socket address.");
        }
        catch (Exception e) {
            throw new ChannelReadException(e);
        }
    }

    public void sendPacket(Packet packet, SocketAddress target) throws ChannelWriteException {
        assert(target != null);

        // FIXME check if it needs additional checks and calls
        try {
            channel.send(Packet.toByteBuffer(packet), target);
        } catch (IOException e) {
            throw new ChannelWriteException(e);
        }
    }

    public void close() {
        try {
            channel.close();
        } catch (IOException e) {
            Log.debug(LogLevel.ERROR, "ServerUdpChannel failed to close gracefully, " + e.getMessage());
        }

        Log.debug(LogLevel.SERVER, "The ServerUdpChannel has closed.");
    }
}
