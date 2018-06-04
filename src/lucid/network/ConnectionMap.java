package lucid.network;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.HashMap;

import lucid.util.Log;
import lucid.util.LogLevel;

public class ConnectionMap {
    private HashMap<Long, Connection> connections = new HashMap<Long, Connection>();
    
    public int size() {
    	return connections.size();
    }
    
    public Collection<Connection> getConnections() {
    	return connections.values();
    }
    
    /** Remove a connection from the array */
    public final void remove(long unique) {
        connections.remove(unique);
    }
    
	public Connection getFromAddress(SocketAddress address) {
		Connection connection = null;
		for (Connection c: connections.values()) {
			if (c.udp != null) {
				if (c.udp.getAddress() == address) {
					connection = c;
					break;
				}
			}
		}
		return connection;
	}
	
	public Connection getFromUnique(long unique) {
		if (connections.containsKey(unique)) {
			return connections.get(unique);
		}
		return null;
	}
	
	public boolean addTcpToConnection(TcpConnection tcp) {
		Connection connection = getFromUnique(tcp.getUnique());
		
		if (connection != null) {
			// If connection has no TCP, add it, otherwise reject the connection
			if (connection.tcp == null) {
				connection.tcp = tcp;
			}
			else {
				Log.debug(LogLevel.SERVER, "Client already connected via UDP");
				return false;
			}
		}
		else {
			// If there is no connection, make a new connection and add TCP to it
			connection = new Connection();
			connection.tcp = tcp;
			connections.put(tcp.getUnique(), connection);
		}
		return true;
	}
	
	public boolean addUdpToConnection(UdpConnection udp) {
		Connection connection = getFromUnique(udp.getUnique());
		
		if (connection != null) {
			// If connection has no UDP, add it, otherwise reject the connection
			if (connection.udp == null) {
				connection.udp = udp;
			}
			else {
				Log.debug(LogLevel.SERVER, "Client already connected via UDP");
				return false;
			}
		}
		else {
			// If there is no connection, make a new connection and add UDP to it
			connection = new Connection();
			connection.udp = udp;
			connections.put(udp.getUnique(), connection);
		}
		return true;
	}
}
