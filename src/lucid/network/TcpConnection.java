package lucid.network;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import lucid.exceptions.TcpReadException;
import lucid.exceptions.TcpWriteException;
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
	private TcpChannel channel;
	
	/** Whether the client is listening or not */
	public boolean connected = false;

	public TcpConnection(SocketChannel channel) {
		this.channel = new TcpChannel(channel);
		
		connected = true;
	}
	
	public TcpChannel getChannel() {
		return channel;
	}
	
	public long getUnique() {
		return unique;
	}
	
	public void setUnique(long unique) {
		this.unique = unique;
	}
	
	public Packet getPacket() {
		return channel.receive();
	}
	
	/**
	 * Read from the TCP channel of this connection.
	 * @throws TcpReadException If reading from the channel failed
	 */
	public void read() throws TcpReadException {
		channel.read();
	}
	
	public void write() throws TcpWriteException {
		if (connected) {
			if (channel.hasUnsentData()) {
				channel.write();
			}
		}
	}
	
	public void send(Packet packet) {
		channel.send(packet);
		Log.debug(LogLevel.SPACKET, "Sent to: " + unique + " , " + packet);
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
