package lucid.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import lucid.Config;
import lucid.util.Log;
import lucid.util.LogLevel;

public class ServerUdpChannel {
	/** The channel this server will listen on for UDP connections */
	private DatagramChannel channel;
	
	/** The port this server will listen on for UDP connections */
	private int port;
	
	/** The packet buffer that caches incoming packets */
	private PacketBuffer in = new PacketBuffer(Config.READ_BUFFER, "UDP Server");
	
	/** The selector which will select when this channel is ready accept */
	private Selector selector;
	
	public ServerUdpChannel(int port, Selector selector) {
		this.port = port;
		this.selector = selector;
	}
	
	/** This method will open the channel and attach the channel to the given selector */
	public void start() throws Exception {
		channel = DatagramChannel.open();
		channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
		channel.socket().bind(new InetSocketAddress(port));
		channel.configureBlocking(false);
		SelectionKey key = channel.register(selector, SelectionKey.OP_READ);
		key.attach(this);
	}
	
	/** This method will accept a new connection if one is available
	 *
	 *  @return  returns the new connection or null if no connection could be accepted
	 */
	public UdpConnection accept(Packet handshake) {
		try {
			if (handshake == null) {
				return null;
			}
			
			long unique = handshake.getLong();
			Log.debug(LogLevel.SERVER, "Got a new handshake from client with id: " + unique);
			ByteBuffer hOut = Packet.toByteBuffer(handshake);
			channel.send(hOut, handshake.getSource());
			
			try {
				DatagramChannel client = DatagramChannel.open();
				client.configureBlocking(false);
				
				UdpConnection connection = new UdpConnection(client, handshake.getSource());
				
				connection.setUnique(unique);
				connection.connect();
				
				return connection;
			} catch(IOException e) {
				Log.debug(LogLevel.ERROR, "Failed to open new UDP connection on server");
			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.debug(LogLevel.ERROR, "An error occurred in ServerUdpChannel:accept(), " + e.getMessage());
			close();
		}

		return null;
	}
	
	public Packet getPacket() {
		return in.get();
	}
	
	public void read() throws IOException {
		try {
			in.readUdp(channel);
		} catch(IOException e) {
			close();
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
