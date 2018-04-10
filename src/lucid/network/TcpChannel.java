package lucid.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import lucid.exceptions.TcpReadException;
import lucid.exceptions.TcpWriteException;
import lucid.util.Log;
import lucid.util.LogLevel;

public class TcpChannel {
	private SocketChannel channel;
	
	/** The channel output buffer */
	private ByteBuffer writeBuffer = ByteBuffer.allocate(8192); // TODO make configurable
	private ByteBuffer readBuffer = ByteBuffer.allocate(8192); // TODO make configurable
	
	private BlockingQueue<Packet> incomingPackets = new LinkedBlockingQueue<Packet>(); // TODO add max size + size warning + timeout on full
	private BlockingQueue<Packet> outgoingPackets = new LinkedBlockingQueue<Packet>(); // TODO add max size + size warning + timeout on full

	public TcpChannel(SocketChannel channel) {
		this.channel = channel;
	}
	
	public boolean isConnected() {
		return channel.isConnected();
	}
	
	public void setBlocking(boolean blocking) {
		try {
			channel.configureBlocking(blocking);
		} catch (IOException e) {
			Log.debug(LogLevel.ERROR, "Failed to configure blocking mode on TCP channel.");
		}
	}
	
	public boolean hasUnsentData() {
		return writeBuffer.position() > 0 || !outgoingPackets.isEmpty();
	}

	public void send(Packet packet) {
		outgoingPackets.offer(packet);
	}
	
	public void write() throws TcpWriteException {
		// Fill up the write buffer with queued up packets
		while (!outgoingPackets.isEmpty()) {
			Packet packet = outgoingPackets.peek();
			if (writeBuffer.remaining() < packet.getLength() + Packet.HEADER_SIZE) {
				break;
			}
			outgoingPackets.remove();
			
			writeBuffer.put(packet.getData());
		}
		
		// Send the write buffer through the channel
		try {
			writeBuffer.flip();
			
			int bytesToWrite = writeBuffer.remaining();
			int bytesWritten = channel.write(writeBuffer);
			
			if (bytesWritten != bytesToWrite) {
				// Copy the remaining bytes to the start of the buffer
				writeBuffer.compact();
				Log.debug(LogLevel.ERROR, "Did not write all bytes to socket channel.");
			}
			else {
				writeBuffer.clear();
			}
		} catch (NotYetConnectedException e) {
			throw new TcpWriteException("Tried to read from an unconnected TCP channel.");
		} catch (ClosedByInterruptException e) {
			throw new TcpWriteException("Thread got interrupted while reading from TCP channel.");
		} catch (AsynchronousCloseException e) {
			throw new TcpWriteException("Another thread closed TCP channel while reading.");
		} catch (ClosedChannelException e) {
			throw new TcpWriteException("Tried to read from a closed TCP channel.");
		} catch (IOException e) {
			throw new TcpWriteException("An IO exception occurred while reading from TCP channel.");
		}
	}
	
	public boolean hasPackets() {
		return !incomingPackets.isEmpty();
	}
	
	public Packet receive() {
		return incomingPackets.poll();
	}

	/**
	 * Reads a number of bytes from the channel and store it as a packet in the buffer.
	 * @param channel The channel to read from
	 * @return The number of bytes read
	 * @throws TcpReadException If reading from the channel failed
	 */
	public int read() throws TcpReadException {
		try {
			int bytesRead = channel.read(readBuffer);
			
			if (bytesRead == 0) {
				return 0;
			}
			if (bytesRead < 0) {
				throw new TcpReadException("TCP channel reached end-of-stream.");
			}
			
			// Flip the buffer to set the limit to the number of bytes received
			readBuffer.flip();

			while (readBuffer.remaining() > Packet.HEADER_SIZE) {
				Packet packet = Packet.fromByteBuffer(readBuffer);
				if (packet == null) {
					break;
				}
				boolean added = incomingPackets.offer(packet);
				
				if (!added) {
					Log.debug(LogLevel.ERROR, "The input packet buffers capacity has been exceeded.");
				}
			}
			
			// Check if we reached end-of-stream or if there is still a fragment of a packet left
			if (readBuffer.remaining() > 0) {
				// Copy remaining fragment of packet to start of buffer
				byte[] toCopy = new byte[readBuffer.remaining()];
				readBuffer.get(toCopy);
				readBuffer.clear();
				readBuffer.put(toCopy);
			}
			else {
				// End-of-stream reached, clear buffer
				readBuffer.clear();
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
	
	public void close() throws IOException {
		channel.close();
	}
}
