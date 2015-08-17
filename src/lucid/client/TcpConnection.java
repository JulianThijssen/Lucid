package lucid.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

import lucid.network.Packet;
import lucid.network.PacketBuffer;
import lucid.util.UniqueGenerator;

public class TcpConnection implements Runnable {
	public static int sid = 0;
	
	private int id = sid;
	
    /** The connection of the client to the server */
	private SocketChannel channel;
	
	/** The channel input buffer */
	private PacketBuffer in = new PacketBuffer(1024, "TCP Client " + id);
	
	/** The channel output buffer */
	private ByteBuffer out = ByteBuffer.allocate(1024);
    
    /** Whether the client is listening or not */
    private boolean connected = false;
    
    /** List of all the listeners that get notified of connection events */
    private List<NetworkListener> listeners = new ArrayList<NetworkListener>();
    
	public boolean connect(String host, int port) {
		try {
			channel = SocketChannel.open();
			channel.connect(new InetSocketAddress(host, port));
			channel.setOption(StandardSocketOptions.TCP_NODELAY, true);
			
			Packet packet = new Packet(0);
			packet.addLong(UniqueGenerator.unique);
			send(packet);
			read();
			getPacket();
			System.out.println("TCP client handshake successful");
	    } catch(IOException e) {
	    	e.printStackTrace();
	        purge();
	        //TODO Log.debug(String.format("Failed to connect to host: %s at port: %d", host, port));
	        return false;
	    }
		connected = true;
		
		new Thread(this).start();
		notifyConnected();
		//TODO Log.debug(String.format("Successfully connected to host: %s at port: %d", host, port));
		
		return true;
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
	
	public void addListener(NetworkListener listener) {
		listeners.add(listener);
	}
	
	private void listen() {
		if(!channel.isConnected()) {
			purge();
		}

		read();
		
		Packet packet = null;
		
		while ((packet = getPacket()) != null) {
			notifyReceived(packet);
		}
	}
	
	public void send(Packet packet) {
		try {
			out.put(packet.getData());
			out.flip();
			channel.write(out);
			out.clear();
		} catch(IOException e) {
			purge();
		}
	}
	
	private void read() {
		try {
			in.readTcp(channel);
		} catch (AsynchronousCloseException e) {
			// Silently continue
		} catch (Exception e) {
			e.printStackTrace();
			purge();
		}
	}
	
	public Packet getPacket() {
		return in.get();
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
	
	public void purge() {
		try {
			connected = false;
			if (channel != null) { channel.close(); }
			notifyDisconnected();
	    } catch (IOException e) {
	    	//TODO Log.debug("Connection to the server was closed");
	    }
	}
}
