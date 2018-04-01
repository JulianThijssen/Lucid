package lucid.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import lucid.util.Log;
import lucid.util.LogLevel;

public class PacketBuffer {
	private ByteBuffer in = null;
	private BlockingQueue<Packet> packets = new LinkedBlockingQueue<Packet>();
	
	public String source = null;
	
	public PacketBuffer(int capacity, String source) {
		in = ByteBuffer.allocate(capacity);
		this.source = source;
	}
	
	public Packet get() {
		return packets.poll();
	}
	
	public synchronized int readUdp(DatagramChannel channel) throws IOException {
		int bytesRead = 0;
		in.clear();

		InetSocketAddress sourceAddress = (InetSocketAddress) channel.receive(in);
		bytesRead = in.position();

		in.flip();
		
		Packet packet = null;

		do {
			packet = extract(in);
			if (packet != null) {
				packet.setSource(sourceAddress);
				packets.offer(packet);
			}
		} while (packet != null);
		
		return bytesRead;
	}
	
	/**
	 * Reads a number of bytes from the channel and store it as a packet in the buffer.
	 * @param channel The channel to read from
	 * @return The number of bytes read
	 * @throws TcpReadException If reading from the channel failed
	 */
	public int readTcp(SocketChannel channel) throws TcpReadException {
		try {
			int bytesRead = channel.read(in);

			if (bytesRead == 0) {
				return 0;
			}
			if (bytesRead < 0) {
				throw new TcpReadException("TCP channel reached end-of-stream.");
			}
			
			// Flip the buffer to set the limit to the number of bytes received
			in.flip();

			Packet packet = null;
			while (in.remaining() > 0 && (packet = Packet.fromByteBuffer(in)) != null) {
				boolean added = packets.offer(packet);

				if (!added) {
					Log.debug(LogLevel.ERROR, "The input packet buffers capacity has been exceeded." + " : " + source);
				}
			}
			
			// Check if we reached end-of-stream or if there is still a fragment of a packet left
			if (in.remaining() > 0) {
				// Copy remaining fragment of packet to start of buffer
				byte[] toCopy = new byte[in.remaining()];
				in.get(toCopy);
				in.clear();
				in.put(toCopy);
			}
			else {
				// End-of-stream reached, clear buffer
				in.clear();
			}
			
			return bytesRead;
		} catch (NotYetConnectedException e) {
			throw new TcpReadException("Tried to read from an unconnected TCP channel.");
		} catch (ClosedByInterruptException e) {
			throw new TcpReadException("Thread got interrupted while reading from TCP channel.");
		} catch (AsynchronousCloseException e) {
			throw new TcpReadException("Another thread closed TCP channel while reading.");
		} catch (ClosedChannelException e) {
			throw new TcpReadException("Tried to read from a closed TCP channel.");
		} catch (IOException e) {
			throw new TcpReadException("An IO exception occurred while reading from TCP channel.");
		}
	}
}
