package lucid.network;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;

import lucid.exceptions.ChannelReadException;
import lucid.exceptions.ChannelWriteException;
import lucid.exceptions.ConnectionException;
import lucid.util.Log;
import lucid.util.LogLevel;

public class UdpConnection extends AbstractConnection {
    /** Debug messages */
    public static final String STREAM_SUCCESS = "Successfully opened streams on client connection.";
    public static final String STREAM_FAILURE = "Failed to open stream on client. Closing connection..";
    public static final String LISTEN_FAILURE = "An error occurred while listening. Closing connection..";
    public static final String CLOSE_SUCCESS = "Successfully closed client connection, removing client..";
    public static final String CLOSE_FAILURE = "Failed to close the client connection elegantly, discarding client..";

    /** The datagram channel on which packets are received */
    private UdpChannel channel;

    /** The address with which the client is connected */
    private SocketAddress address = null;

    public UdpConnection(DatagramChannel channel, SocketAddress address) {
        this.channel = new UdpChannel(channel);
        this.address = address;
    }

    public UdpChannel getChannel() {
        return channel;
    }

    public SocketAddress getAddress() {
        return address;
    }

    public boolean hasPackets() {
        return channel.hasPackets();
    }

//  public Packet readPacket() {
//      return channel.receive();
//  }

    public void connect() throws ConnectionException {
        try {
            channel.connect(address);

            connected = true;
        } catch (IOException e) {
            close();
            throw new ConnectionException("Failed to connect UDP connection to client address");
        }
    }

    public void sendPacket(Packet packet) {
        channel.send(packet);
    }

    /**
     * Read from the UDP channel of this connection.
     * @throws ChannelReadException If reading from the channel failed
     */
//  public void read() throws ChannelReadException {
//      channel.read();
//  }

    public void write() throws ChannelWriteException {
        if (connected) {
            channel.write();
        }
    }

    public void close() {
        try {
            connected = false;
            channel.close();
        } catch (IOException e) {
            Log.debug(LogLevel.ERROR, CLOSE_FAILURE);
        }
        // TODO server.notifyDisconnection(this);
        // TODO server.removeConnection(id);

        Log.debug(LogLevel.SERVER, "UDP connection has disconnected");
    }
}
