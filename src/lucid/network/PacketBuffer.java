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
	
	public int readTcp(SocketChannel channel) throws AsynchronousCloseException, Exception {
		int bytesRead = 0;
		in.clear();
		
		channel.read(in);
		bytesRead = in.position();
		
		if (bytesRead == 0) {
			throw new AsynchronousCloseException();
		}

		in.flip();
		
		Packet packet = null;
		do {
			packet = extract(in);
			if (packet != null) {
				boolean added = packets.offer(packet);

				if (!added) {
					Log.debug(LogLevel.ERROR, "The input packet buffers capacity has been exceeded." + " : " + source);
				}
			}
		} while (packet != null);
		return bytesRead;
	}
	
	private Packet extract(ByteBuffer in) {
		try {
			if (in.position() == in.limit()) {
				return null;
			}

			int type = in.get();
			if (type == -1) {
				return null;
			}

			int len = in.getShort();

			Packet packet = new Packet(type);
			byte[] b = new byte[len];
			for(int i = 0; i < len; i++) {
				b[i] = in.get();
			}
			packet.setData(b);

			return packet;
		} catch(BufferUnderflowException e) {
			Log.debug(LogLevel.ERROR, "Received a broken packet" + " : " + source);
			return null;
		}
	}
}
