package lucid.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import lucid.Config;
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
	
	public void start() throws Exception {
		channel = ServerSocketChannel.open();
		channel.socket().setReuseAddress(true);
		channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
		channel.socket().bind(new InetSocketAddress(port), Config.MAX_PENDING);
		channel.configureBlocking(false);
		channel.register(selector, SelectionKey.OP_ACCEPT);
	}
	
	public TcpConnection accept() throws IOException {
		SocketChannel client = channel.accept();
		//client.configureBlocking(false);
		client.setOption(StandardSocketOptions.TCP_NODELAY, true);
		
		TcpConnection connection = new TcpConnection(client);
		
		try {
			connection.read();
		} catch (Exception e) {
			connection.close();
			return null;
		}
		Packet handshake = connection.getPacket();
		
		Log.debug(LogLevel.SPACKET, "Handshake: " + handshake);
		
		connection.setUnique(handshake.getLong());
		connection.send(handshake);
		
		connection.getChannel().configureBlocking(false);
		SelectionKey key = client.register(selector, SelectionKey.OP_READ);
		key.attach(connection);
		Log.debug(LogLevel.ERROR, "New: " + connection.getUnique());
		return connection;
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
