package lucid.network;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetConnectedException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.IntSupplier;

import lucid.exceptions.ChannelReadException;
import lucid.exceptions.ChannelWriteException;
import lucid.util.Log;
import lucid.util.LogLevel;
import lucid.util.Statistics;

public class PacketBuffer {
	/** The channel output buffer */
	protected ByteBuffer writeBuffer = ByteBuffer.allocate(8192); // TODO make configurable, and do not violate channel limits
	protected ByteBuffer readBuffer = ByteBuffer.allocate(8192); // TODO make configurable, and do not violate channel limits
	
	protected BlockingQueue<Packet> incomingPackets = new LinkedBlockingQueue<Packet>(); // TODO add max size + size warning + timeout on full
	protected BlockingQueue<Packet> outgoingPackets = new LinkedBlockingQueue<Packet>(); // TODO add max size + size warning + timeout on full

	public PacketBuffer() {
		Statistics.inPacketBufferSize = new IntSupplier() {
			@Override
			public int getAsInt() {
				return incomingPackets.size();
			}
		};
		Statistics.outPacketBufferSize = new IntSupplier() {
			@Override
			public int getAsInt() {
				return outgoingPackets.size();
			}
		};
	}
	
	public boolean hasUnsentData() {
		return writeBuffer.position() > 0 || !outgoingPackets.isEmpty();
	}

	public void send(Packet packet) {
		outgoingPackets.offer(packet);
	}
	
	public boolean hasPackets() {
		return !incomingPackets.isEmpty();
	}
	
	public Packet receive() {
		return incomingPackets.poll();
	}
	
	public void write(ChannelWriter writer) throws ChannelWriteException {
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
			int bytesWritten = writer.write(writeBuffer);

			if (bytesWritten != bytesToWrite) {
				// Copy the remaining bytes to the start of the buffer
				writeBuffer.compact();
				Log.debug(LogLevel.ERROR, "Did not write all bytes to socket channel.");
			}
			else {
				writeBuffer.clear();
			}
		} catch (NotYetConnectedException e) {
			throw new ChannelWriteException("Tried to read from an unconnected channel.");
		} catch (ClosedByInterruptException e) {
			throw new ChannelWriteException("Thread got interrupted while reading from channel.");
		} catch (AsynchronousCloseException e) {
			throw new ChannelWriteException("Another thread closed channel while reading.");
		} catch (ClosedChannelException e) {
			throw new ChannelWriteException("Tried to read from a closed channel.");
		} catch (IOException e) {
			throw new ChannelWriteException("An IO exception occurred while reading from channel.");
		}
	}

	/**
	 * Reads a number of bytes from the channel and store it as a packet in the buffer.
	 * @param channel The channel to read from
	 * @return The number of bytes read
	 * @throws ChannelReadException If reading from the channel failed
	 */
	public int read(SocketAddress address, ChannelReader reader) throws ChannelReadException {
		try {
			int bytesRead = reader.read(readBuffer);

			if (bytesRead == 0) {
				return 0;
			}
			if (bytesRead < 0) {
				throw new ChannelReadException("TCP channel reached end-of-stream.");
			}

			// Flip the buffer to set the limit to the number of bytes received
			readBuffer.flip();

			while (readBuffer.remaining() > Packet.HEADER_SIZE) {
				Packet packet = Packet.fromByteBuffer(readBuffer);
				if (packet == null) {
					break;
				}
				packet.setSource(address);
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
			throw new ChannelReadException("Tried to read from an unconnected channel.");
		} catch (ClosedByInterruptException e) {
			throw new ChannelReadException("Thread got interrupted while reading from channel.");
		} catch (AsynchronousCloseException e) {
			throw new ChannelReadException("Another thread closed channel while reading.");
		} catch (ClosedChannelException e) {
			throw new ChannelReadException("Tried to read from a closed channel.");
		} catch (IOException e) {
			throw new ChannelReadException("An IO exception occurred while reading from channel.");
		}
	}
}
