package lucid.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.List;

import lucid.network.Packet;
import lucid.network.PacketBuffer;
import lucid.util.UniqueGenerator;

public class UdpConnection implements Runnable {
    /** The connection of the client to the server */
	private DatagramChannel channel;
	
	/** The channel input buffer */
	private PacketBuffer in = new PacketBuffer(1024, "UDP Client");
	
	/** The channel output buffer */
	private ByteBuffer out = ByteBuffer.allocate(1024);
	
	private InetSocketAddress address = null;
    
    /** Whether the client is listening or not */
    private boolean connected = false;
    
    /** List of all the listeners that get notified of connection events */
    private List<NetworkListener> listeners = new ArrayList<NetworkListener>();
    
	public boolean connect(String host, int port) {
		address = new InetSocketAddress(host, port);
		
		try {
			channel = DatagramChannel.open();
			connected = handshake(address);
			
			channel.configureBlocking(false);
			
			if (!connected) {
				close();
				return false;
			}
	    } catch(IOException e) {
	    	e.printStackTrace();
	        close();
	        //TODO Log.debug(String.format("Failed to connect to host: %s at port: %d", host, port));
	        return false;
	    }
		
		new Thread(this).start();
		notifyConnected();
		//TODO Log.debug(String.format("Successfully connected to host: %s at port: %d", host, port));
		
		return true;
	}
	
	private boolean handshake(InetSocketAddress address) {
		try {
			Packet hPacket = new Packet(0);
			hPacket.addLong(UniqueGenerator.unique);
			
			ByteBuffer hOut = Packet.toByteBuffer(hPacket);

			channel.socket().setSoTimeout(1000);
			int bytes = channel.send(hOut, address);

			read();
			getPacket();
			System.out.println("UDP client handshake successful");
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean isConnected() {
		return connected;
	}

	@Override
	public void run() {
		while(connected) {
			listen();
		}
	}
	
	private void listen() {
		try {
			int bytesRead = read();
			
			Packet packet = null;
			
			while ((packet = getPacket()) != null) {
				notifyReceived(packet);
			}
		} catch (IOException e) {
			close();
		}
	}
	
	public Packet getPacket() {
		return in.get();
	}
	
	public int read() throws IOException {
		return in.readUdp(channel);
	}
	
	public void send(Packet packet) {
		out.put(packet.getData());
		out.flip();
		try {
			channel.send(out, address);
			out.clear();
		} catch(IOException e) {
			e.printStackTrace();
			close();
		}
		System.out.println("[Client] Done sending");
	}
	
	public void addListener(NetworkListener listener) {
		listeners.add(listener);
	}
	
	public void notifyConnected() {
		for(NetworkListener listener: listeners) {
			listener.connected();
		}
	}
	
	public void notifyReceived(Packet packet) {
		for(NetworkListener listener: listeners) {
			listener.received(packet);
		}
	}
	
	public void notifyDisconnected() {
		for(NetworkListener listener: listeners) {
			listener.disconnected();
		}
	}
	
	public void close() {
		try {
			connected = false;
			if (channel != null) { channel.close(); }
			notifyDisconnected();
	    } catch(IOException e) {
	    	//TODO Log.debug("Connection to the server was closed");
	    }
		System.out.println("[Client] PURGED!");
	}
}
