package lucid.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import lucid.Config;
import lucid.util.Log;
import lucid.util.LogLevel;

public class TcpConnection {
	/** Debug messages */
	public static final String STREAM_SUCCESS = "Successfully opened streams on client connection.";
	public static final String STREAM_FAILURE = "Failed to open stream on client. Closing connection..";
	public static final String LISTEN_FAILURE = "An error occurred while listening. Closing connection..";
	public static final String CLOSE_SUCCESS = "Successfully closed client connection, removing client..";
	public static final String CLOSE_FAILURE = "Failed to close the client connection elegantly, discarding client..";
	
	/** A unique number identifying this connection from others on the same IP */
	private long unique = -1;
	
	/** The connection of the client to the server */
	private SocketChannel channel;
	
	/** The packet buffer that caches incoming packets */
	private PacketBuffer in = new PacketBuffer(Config.READ_BUFFER, "TCP server conn");
	
	/** The channel output buffer */
	private ByteBuffer out = ByteBuffer.allocate(Config.WRITE_BUFFER);
	
	/** Whether the client is listening or not */
	public boolean connected = false;

	public TcpConnection(SocketChannel channel) {
		this.channel = channel;
		
		connected = true;
	}
	
	public SocketChannel getChannel() {
		return channel;
	}
	
	public long getUnique() {
		return unique;
	}
	
	public void setUnique(long unique) {
		this.unique = unique;
	}
	
	public Packet getPacket() {
		return in.get();
	}
	
	/**
	 * Read from the TCP channel of this connection.
	 * @throws TcpReadException If reading from the channel failed
	 */
	public void read() throws TcpReadException {
		in.readTcp(channel);
	}
	
	public void send(Packet packet) {
		if (connected) {
			try {
				out.put(packet.getData());
				out.flip();
				channel.write(out);
				out.clear();
			} catch(IOException e) {
				close();
			}
			Log.debug(LogLevel.SPACKET, "Sent to: " + unique + " , " + packet);
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
