package lucid.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.List;

import lucid.exceptions.ChannelReadException;
import lucid.exceptions.ChannelWriteException;
import lucid.network.Packet;
import lucid.network.UdpChannel;
import lucid.util.Log;
import lucid.util.LogLevel;
import lucid.util.UniqueGenerator;

public class UdpConnection implements Runnable {
	/** The connection of the client to the server */
	private UdpChannel channel;
	
	private InetSocketAddress address = null;
    
    /** Whether the client is listening or not */
    private boolean connected = false;
    
    /** List of all the listeners that get notified of connection events */
    private List<NetworkListener> listeners = new ArrayList<NetworkListener>();
    
	public boolean connect(String host, int port) {
		address = new InetSocketAddress(host, port);

		try {
			DatagramChannel datagramChannel = DatagramChannel.open();
			datagramChannel.connect(address);
			datagramChannel.configureBlocking(false);

			channel = new UdpChannel(datagramChannel);
			connected = handshake(address);

			if (!connected) {
				close();
				return false;
			}
	    } catch(IOException e) {
	    	e.printStackTrace();
	        close();
	        Log.debug(LogLevel.CLIENT, String.format("Failed to connect to host: %s at port: %d", host, port));
	        return false;
	    }

		new Thread(this).start();
		notifyConnected();
		Log.debug(LogLevel.CLIENT, String.format("Successfully connected to host: %s at port: %d", host, port));
		
		return true;
	}
	
	private boolean handshake(InetSocketAddress address) {
		try {
			Packet handshake = new Packet((short) 0);
			handshake.addLong(UniqueGenerator.unique);
			
			channel.send(handshake);
			try {
				channel.write();
			}
			catch (ChannelWriteException e) {
				Log.debug(LogLevel.ERROR, e.getMessage());
				close();
			}

			// Timeout system FIXME
			long time = 0;
			long timeout = 3000;
			while (!channel.hasPackets()) {
				channel.read();
				
				Thread.sleep(10);
				time += 10;
				if (time > timeout) {
					return false;
				}
			}

			channel.receive();
			Log.debug(LogLevel.CPACKET, "UDP handshake successful");
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
		if (!channel.isConnected()) {
			close();
			return;
		}

		read();
		
		while (channel.hasPackets()) {
			Packet packet = channel.receive();
			notifyReceived(packet);
		}
	}
	
	public void send(Packet packet) {
		channel.send(packet);
		try {
			channel.write();
		} catch (ChannelWriteException e) {
			Log.debug(LogLevel.ERROR, e.getMessage());
			close();
		}
		
		Log.debug(LogLevel.CPACKET, "Done sending packet");
	}
	
	private void read() {
		try {
			channel.read();
		} catch (ChannelReadException e) {
			Log.debug(LogLevel.ERROR, e.getMessage());
			close();
		}
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
		Log.debug(LogLevel.CLIENT, "Connection closed");
	}
}
