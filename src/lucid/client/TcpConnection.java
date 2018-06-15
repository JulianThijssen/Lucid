package lucid.client;

import static lucid.network.ConnectionStatus.CONNECTED;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ConnectionPendingException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.nio.channels.UnsupportedAddressTypeException;

import lucid.exceptions.ConnectionException;
import lucid.network.Packet;
import lucid.network.TcpChannel;
import lucid.util.Log;
import lucid.util.LogLevel;
import lucid.util.UniqueGenerator;

public class TcpConnection extends ClientConnection {
    public boolean connect(String host, int port) throws ConnectionException {
        try {
            Log.debug(LogLevel.CLIENT, String.format("Attempting to connect to host: %s at port %d", host, port));
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.connect(new InetSocketAddress(host, port));
            socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
            
            channel = new TcpChannel(socketChannel);
            Log.debug(LogLevel.CLIENT, "Handshaking...");
            Packet packet = new Packet((short) 0);
            packet.addLong(UniqueGenerator.unique);
            send(packet);
            write();
            read();
            channel.receive();
            Log.debug(LogLevel.CLIENT, "Selector");
            socketChannel.configureBlocking(false);
            
            selector = Selector.open();
            SelectionKey key = socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            key.attach(this);
            
            Log.debug(LogLevel.CLIENT, "TCP handshake successful");
        } catch (AlreadyConnectedException ace) {
            throw new ConnectionException("Already connected to host");
        } catch (ConnectionPendingException cpe) {
            throw new ConnectionException("A non-blocking operation is already in progress on this channel.");
        } catch (ClosedChannelException cce) {
            throw new ConnectionException("The channel is closed.");
        } catch (UnsupportedAddressTypeException asce) {
            throw new ConnectionException("The type of the given remote address is not supported.");
        } catch (UnresolvedAddressException uae) {
            throw new ConnectionException("The given remote address is not fully resolved.");
        } catch (SecurityException se) {
            throw new ConnectionException("The security manager does not permit access to remote endpoint.");
        } catch (ConnectException ce) {
            throw new ConnectionException(String.format("The host: %s at port: %d is not online.", host, port));
        } catch (IOException e) {
            throw new ConnectionException(String.format("IO Exception occurred while connecting to: %s at port: %d", host, port));
        }

        setConnectionStatus(CONNECTED);

        start();
        notifyConnected();
        Log.debug(LogLevel.CLIENT, String.format("Successfully connected to host: %s at port: %d", host, port));

        return true;
    }
    
    @Override
    public void onPacketReceived(Packet packet) {
        notifyReceived(packet);
    }
}
