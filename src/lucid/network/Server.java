package lucid.network;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import lucid.Config;
import lucid.util.Log;
import lucid.util.LogLevel;

public abstract class Server implements Runnable {
	/** The TCP channel bound to a port which receives TCP client connection requests. */
	private ServerTcpChannel tcpChannel;
	
	/** The UDP channel bound to a port which receives UDP client connection requests. */
	private ServerUdpChannel udpChannel;
	
	/** The Selector which will handle multiple connection requests */
	private Selector selector;
	
	/** The TCP port of the server, this is the port players must connect through. */
	private int tcpPort = 0;
	
	/** The UDP port of the server, this is the port players must connect through. */
	private int udpPort = 0;
	
    /** A list keeping track of all the client currently connected to the server. */
    private HashMap<Long, Connection> connections = new HashMap<Long, Connection>();

    /** List of all the listeners that get notified of connection events */
    private List<ServerListener> listeners = new ArrayList<ServerListener>();
    
    /** List of all the listeners that get notified of error events */
    private List<ServerErrorListener> errorListeners = new ArrayList<ServerErrorListener>();
    
	/** Whether the server is running or not */
	private boolean running = false;
	
	public Server(int tcpPort, int udpPort) {
		this.tcpPort = tcpPort;
		this.udpPort = udpPort;
		Config.loadConfig();
	}

	/** Initializes the server to be ready for incoming connections. */
	public void start() {
		if (running) {
			notifyError(ServerError.SERVER_ALREADY_STARTED);
			return;
		}
		try {
			selector = Selector.open();
			
			if (tcpPort != 0) {
				tcpChannel = new ServerTcpChannel(tcpPort, selector);
				tcpChannel.start();
			}
			
			if (udpPort != 0) {
				udpChannel = new ServerUdpChannel(udpPort, selector);
				udpChannel.start();
			}
			
			running = true;
			new Thread(this).start();
			notifyServerStart();
		} catch(IllegalArgumentException e) {
			e.printStackTrace();
			notifyError(ServerError.PORT_OUT_OF_RANGE);
			close();
		} catch(Exception e) {
			e.printStackTrace();
			notifyError(ServerError.SERVER_START_FAILURE);
			close();
		}
	}

	public void run() {
		while(running) {
			listen();
		}
	}
	
	public boolean isRunning() {
		return running;
	}
	
    /** Listens to incoming connection requests from clients and passes them in a Connection class */
    private void listen() {
    	try {
			selector.select();
			Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();

			while (iterator.hasNext()) {
				SelectionKey key = (SelectionKey) iterator.next();

				if (key.isAcceptable()) {
					if (connections.size() < Config.MAX_PLAYERS) {
						TcpConnection tcp = tcpChannel.accept();
						
						if (tcp != null) {
							Connection connection = connections.get(tcp.getUnique());
							if (connection == null) {
								// If there is no connection, make a new connection and add TCP to it
								connection = new Connection();
								connection.tcp = tcp;
								connections.put(tcp.getUnique(), connection);
								notifyConnection(connection);
							} else {
								// There is already a connection
								if (connection.tcp == null) {
									// If the connection has no TCP, add TCP to it
									connection.tcp = tcp;
								}
								else {
									// If the connection already has TCP, deny the new connection
									Log.debug(LogLevel.SERVER, "Connection denied, client already connected via TCP");
									key.cancel();
									tcp.close();
								}
							}
						}
					}
				}
				
				else if (key.isReadable()) {
					Object attachment = key.attachment();
					
					if (attachment instanceof TcpConnection) {
						TcpConnection tcp = (TcpConnection) attachment;

						try {
							tcp.read();
						} catch (Exception e) {
							// The connection to this client broke, disconnect them
							tcp.close();
							key.cancel();
							notifyDisconnection(connections.get(tcp.getUnique()));
							removeConnection(tcp.getUnique());
						}
						
						Packet packet = null;
						while ((packet = tcp.getPacket()) != null) {
							if (connections.get(tcp.getUnique()) != null) {
								notifyReceived(connections.get(tcp.getUnique()), packet);
							}
						}
					} else if (attachment instanceof ServerUdpChannel) {
						udpChannel.read();
						
						Packet packet = null;
						while ((packet = udpChannel.getPacket()) != null) {
							if (packet.getType() == 0) {
								UdpConnection udp = udpChannel.accept(packet);
								
								Connection connection = connections.get(udp.getUnique());
								if (connection == null) {
									connection = new Connection();
									connection.udp = udp;
									
									connections.put(udp.getUnique(), connection);
									notifyConnection(connection);
								} else {
									connection.udp = udp;
								}
							} else {
								Connection connection = null;
								for (Connection conn: connections.values()) {
									if (conn.udp != null) {
										if (conn.udp.getAddress() == packet.getSource()) {
											connection = conn;
											break;
										}
									}
								}
								
								if (connection != null) {
									notifyReceived(connection, packet);
								} else {
									Log.debug(LogLevel.ERROR, "This ain't good!"); // TODO
								}
							}
						}
					} else {
						Log.debug(LogLevel.ERROR, "There is an unknown readable channel");
					}
				}
				
				iterator.remove();
			}
    	} catch(IOException e) {
    		Log.debug(LogLevel.ERROR, "An error occurred while selecting channels");
    	}
    }
    
	/** Broadcast a packet to all clients connected to the server */
	public void broadcastTcp(Packet packet) {
		if (packet == null) { return; }
		
		for(Connection connection: connections.values()) {
			if(connection != null) {
				connection.sendTcp(packet);
			}
		}
	}
	
	/** Broadcast a packet to all clients connected to the server */
	public void broadcastUdp(Packet packet) {
		if (packet == null) { return; }
		
		for(Connection connection: connections.values()) {
			if(connection != null) {
				connection.sendUdp(packet);
			}
		}
	}
    
    /** Remove a connection from the array */
    public void removeConnection(long unique) {
        connections.remove(unique);
    }
    
	public void addListener(ServerListener listener) {
		listeners.add(listener);
	}
	
	public void addErrorListener(ServerErrorListener listener) {
		errorListeners.add(listener);
	}
    
    /** Notify all listeners of the server start */
    private void notifyServerStart() {
    	for (ServerListener sl: listeners) {
    		sl.onServerStart();
    	}
    }
    
    /** Notify all listeners of the server stop */
    private void notifyServerStop() {
    	for (ServerListener sl: listeners) {
    		sl.onServerStop();
    	}
    }
    
    /** Notify all listeners of the new connection */
    private void notifyConnection(Connection connection) {
    	for (ServerListener sl: listeners) {
    		sl.onConnection(connection);
    	}
    }
    
    /** Notify all listeners of the disconnection */
    private void notifyDisconnection(Connection connection) {
    	for (ServerListener sl: listeners) {
    		sl.onDisconnect(connection);
    	}
    }
    
    /** Notify all listeners of received packets */
    private void notifyReceived(Connection connection, Packet packet) {
    	for (ServerListener sl: listeners) {
    		sl.onReceived(connection, packet);
    	}
    }
    
    /** Notify all listeners of server error */
    private void notifyError(ServerError error) {
    	for (ServerErrorListener sel: errorListeners) {
    		sel.onServerError(error);
    	}
    }
	
	/** Closes all connections, and shuts down the server. */
	public void close() {
		running = false;
		
		for(Connection connection: connections.values()) {
			if(connection != null) {
				connection.close();
			}
		}
		if (tcpChannel != null) {
			tcpChannel.close();
		}
		if (udpChannel != null) {
			udpChannel.close();
		}
		
		notifyServerStop();
	}
}
