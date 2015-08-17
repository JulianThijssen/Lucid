package lucid.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import lucid.Config;
import lucid.util.Log;

public class UdpConnection {
	/** Debug messages */
	public static final String STREAM_SUCCESS = "Successfully opened streams on client connection.";
	public static final String STREAM_FAILURE = "Failed to open stream on client. Closing connection..";
	public static final String LISTEN_FAILURE = "An error occurred while listening. Closing connection..";
	public static final String CLOSE_SUCCESS = "Successfully closed client connection, removing client..";
	public static final String CLOSE_FAILURE = "Failed to close the client connection elegantly, discarding client..";
	
	/** A unique number identifying this connection from others on the same IP */
	private long unique = -1;
	
	/** The datagram channel on which packets are received */
	private DatagramChannel channel;
	
	/** The address with which the client is connected */
	private InetSocketAddress address = null;
	
	/** The channel output buffer */
	private ByteBuffer out = ByteBuffer.allocate(Config.WRITE_BUFFER);
	
	/** Whether the client is listening or not */
	private boolean connected = false;
	
	public UdpConnection(DatagramChannel channel, InetSocketAddress address) {
		this.channel = channel;
		this.address = address;
	}
	
	public long getUnique() {
		return unique;
	}
	
	public void setUnique(long unique) {
		this.unique = unique;
	}
	
	public DatagramChannel getChannel() {
		return channel;
	}
	
	public InetSocketAddress getAddress() {
		return address;
	}
	
	public void connect() {
		try {
			channel.connect(address);
		} catch (IOException e) {
			Log.error("Failed to set up UDP connection to client");
			close();
		}
		Log.debug("Succesfully set up UDP connection to client: " + address);
	}
	
	public void send(Packet packet) {
		out.put(packet.getData());

		out.flip();
		try {
			channel.write(out);

			out.clear();
		} catch(IOException e) {
			close();
		}
		//Log.debug("Sent UDP packet to client: " + address);
	}
	
	public void close() {
		try {
			connected = false;
			channel.close();
		} catch (IOException e) {
			Log.debug(CLOSE_FAILURE);
		}
		// TODO server.notifyDisconnection(this);
		// TODO server.removeConnection(id);

		Log.debug("UDP connection has disconnected");
	}
}
