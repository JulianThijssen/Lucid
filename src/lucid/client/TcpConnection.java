package lucid.client;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ConnectionPendingException;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.nio.channels.UnsupportedAddressTypeException;
import java.util.ArrayList;
import java.util.List;

import lucid.exceptions.ConnectionException;
import lucid.exceptions.TcpReadException;
import lucid.exceptions.TcpWriteException;
import lucid.network.Packet;
import lucid.network.TcpChannel;
import lucid.util.Log;
import lucid.util.LogLevel;
import lucid.util.UniqueGenerator;

public class TcpConnection implements Runnable {
	public static int sid = 0;
	
	private int id = sid;
	
    /** The connection of the client to the server */
	private TcpChannel channel;
    
    /** Whether the client is listening or not */
    private boolean connected = false;
    
    /** List of all the listeners that get notified of connection events */
    private List<NetworkListener> listeners = new ArrayList<NetworkListener>();
    
	public boolean connect(String host, int port) throws ConnectionException {
		try {
			Log.debug(LogLevel.CLIENT, String.format("Attempting to connect to host: %s at port %d", host, port));
			SocketChannel socketChannel = SocketChannel.open();
			socketChannel.connect(new InetSocketAddress(host, port));
			socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
			
			channel = new TcpChannel(socketChannel);

			Packet packet = new Packet((short) 0);
			packet.addLong(UniqueGenerator.unique);
			send(packet);
			read();
			getPacket();
			Log.debug(LogLevel.CLIENT, "TCP handshake successful");
		} catch (AlreadyConnectedException ace) {
			throw new ConnectionException("Already connected to host");
		} catch (ConnectionPendingException cpe) {
			throw new ConnectionException("A non-blocking operation is already in progress on this channel.");
		} catch (ClosedChannelException cce) {
			throw new ConnectionException("The channel is closed.");
		} catch (UnsupportedAddressTypeException asce) {
			throw new ConnectionException("The type of the given remote address is not supported.");
		} catch (UnresolvedAddressException uae) {
			throw new ConnectionException("The given remote address is not fully resolved.");
		} catch (SecurityException se) {
			throw new ConnectionException("The security manager does not permit access to remote endpoint.");
		} catch (ConnectException ce) {
			throw new ConnectionException(String.format("The host: %s at port: %d is not online.", host, port));
		} catch (IOException e) {
			throw new ConnectionException(String.format("IO Exception occurred while connecting to: %s at port: %d", host, port));
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
			close();
		}

		read();
		
		while (channel.hasPackets()) {
			Packet packet = getPacket();
			notifyReceived(packet);
		}
	}
	
	public void send(Packet packet) {
		channel.send(packet);
		try {
			channel.write();
		} catch (TcpWriteException e) {
			Log.debug(LogLevel.ERROR, e.getMessage());
			close();
		}
	}
	
	private void read() {
		try {
			channel.read();
		} catch (TcpReadException e) {
			Log.debug(LogLevel.ERROR, e.getMessage());
			close();
		}
	}
	
	public Packet getPacket() {
		return channel.receive();
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
	    } catch (IOException e) {
	    	//TODO Log.debug("Connection to the server was closed");
	    }
	}
}
