package lucid.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import lucid.Config;
import lucid.exceptions.ConnectionException;
import lucid.exceptions.PacketException;
import lucid.exceptions.ServerStartException;
import lucid.exceptions.ChannelReadException;
import lucid.exceptions.ChannelWriteException;
import lucid.util.Log;
import lucid.util.LogLevel;

public class ServerTcpChannel {
    /** The TCP channel bound to a port which receives TCP client connection requests. */
    private ServerSocketChannel channel;

    private int port;

    private Selector selector;

    public ServerTcpChannel(int port, Selector selector) {
        this.port = port;
        this.selector = selector;
    }

//  @Override
//  public void setBlocking(boolean blocking) {
//      try {
//          channel.configureBlocking(blocking);
//      } catch (IOException e) {
//          Log.debug(LogLevel.ERROR, "Failed to set blocking mode on Server TCP Channel");
//      }
//  }

//  @Override
//  protected SocketAddress getRemoteAddress() throws IOException {
//      // TODO Auto-generated method stub
//      return null;
//  }

    public void start() throws ServerStartException, Exception {
        InetSocketAddress address;
        try {
            address = new InetSocketAddress(port);
        }
        catch (IllegalArgumentException e) {
            throw new ServerStartException(ServerError.PORT_OUT_OF_RANGE);
        }

        channel = ServerSocketChannel.open();
        channel.socket().setReuseAddress(true);
        channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        channel.socket().bind(address, Config.MAX_PENDING_TCP_CONNECTIONS);
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_ACCEPT);
    }

    public TcpConnection accept() throws ConnectionException {
        TcpConnection connection = null;

        try {
            SocketChannel client = channel.accept();
            client.setOption(StandardSocketOptions.TCP_NODELAY, true);

            connection = new TcpConnection(client);

            // Read handshake from client
            connection.read();

            Packet handshake = connection.readPacket();
            Log.debug(LogLevel.SPACKET, "Handshake: " + handshake);

            long unique = handshake.getLong();
            connection.setUnique(unique);

            // Send it back to show that we accept it
            connection.sendPacket(handshake);
            connection.write();

            // Set options on the new connection
            connection.getChannel().setBlocking(false);
            SelectionKey key = client.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            key.attach(connection);
            Log.debug(LogLevel.ERROR, "New: " + connection.getUnique());

            return connection;
        } catch (ClosedChannelException e) {
            throw new ConnectionException("Tried to accept on closed server TCP channel.");
        } catch (SecurityException e) {
            throw new ConnectionException("Security manager does not permit access to the remote endpoint of new connection.");
        } catch (IOException e) {
            throw new ConnectionException("IO Error occurred on accepting TCP connection.");
        } catch (ChannelReadException e) {
            connection.close();
            throw new ConnectionException(e.getMessage());
        } catch (ChannelWriteException e) {
            connection.close();
            throw new ConnectionException(e.getMessage());
        } catch (PacketException e) {
            connection.close();
            throw new ConnectionException(e.getMessage());
        }
    }

    public void close() {
        try {
            channel.close();
        } catch (IOException e) {
            Log.debug(LogLevel.ERROR, "ServerTcpChannel failed to close gracefully, " + e.getMessage());
        }

        Log.debug(LogLevel.ERROR, "The ServerTcpChannel has closed.");
    }
}
