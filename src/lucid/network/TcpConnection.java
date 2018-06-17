package lucid.network;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import lucid.exceptions.ChannelReadException;
import lucid.exceptions.ChannelWriteException;
import lucid.util.Log;
import lucid.util.LogLevel;

public class TcpConnection extends AbstractConnection {
    /** Debug messages */
    public static final String STREAM_SUCCESS = "Successfully opened streams on client connection.";
    public static final String STREAM_FAILURE = "Failed to open stream on client. Closing connection..";
    public static final String LISTEN_FAILURE = "An error occurred while listening. Closing connection..";
    public static final String CLOSE_SUCCESS = "Successfully closed client connection, removing client..";
    public static final String CLOSE_FAILURE = "Failed to close the client connection elegantly, discarding client..";

    /** The connection of the client to the server */
    private TcpChannel channel;

    public TcpConnection(SocketChannel channel) {
        this.channel = new TcpChannel(channel);

        connected = true;
    }

    public TcpChannel getChannel() {
        return channel;
    }

    public boolean hasPackets() {
        return channel.hasPackets();
    }

    public Packet readPacket() {
        return channel.receive();
    }

    public void sendPacket(Packet packet) {
        channel.send(packet);
    }

    /**
     * Read from the TCP channel of this connection.
     * @throws ChannelReadException If reading from the channel failed
     */
    public void read() throws ChannelReadException {
        channel.read();
    }

    public void write() throws ChannelWriteException {
        if (connected) {
            if (channel.hasUnsentData()) {
                channel.write();
            }
        }
    }

    public void close() {
        try {
            connected = false;
            channel.close();
        } catch(IOException e) {
            Log.debug(LogLevel.SERVER, CLOSE_FAILURE);
        }

        Log.debug(LogLevel.SERVER, "TCP Connection has disconnected");
    }
}
