package lucid.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetConnectedException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.IntSupplier;

import lucid.exceptions.ChannelWriteException;
import lucid.util.Log;
import lucid.util.LogLevel;
import lucid.util.Statistics;

public class PacketOutputBuffer {
    private ByteBuffer writeBuffer = ByteBuffer.allocate(8192); // TODO make configurable, and do not violate channel limits
    
    private BlockingQueue<Packet> outgoingPackets = new LinkedBlockingQueue<Packet>(); // TODO add max size + size warning + timeout on full
    
    private final Object writeLock = new Object();
    
    public PacketOutputBuffer() {
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
        synchronized (writeLock) {
            outgoingPackets.offer(packet);
        }
    }
    
    public void write(ChannelWriter writer) throws ChannelWriteException {
        // Fill up the write buffer with queued up packets
        synchronized (writeLock) {
            while (!outgoingPackets.isEmpty()) {
                Packet packet = outgoingPackets.peek();
                if (writeBuffer.remaining() < packet.getLength() + Packet.HEADER_SIZE) {
                    break;
                }
                outgoingPackets.remove();
    
                writeBuffer.put(packet.getData());
            }
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
}
