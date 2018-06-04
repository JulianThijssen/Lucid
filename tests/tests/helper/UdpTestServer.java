package tests.helper;
import lucid.exceptions.PacketException;
import lucid.network.Connection;
import lucid.network.Packet;
import lucid.network.Server;
import lucid.network.ServerListener;


public class UdpTestServer extends Server implements ServerListener {
	public Connection connection;
	
	public UdpTestServer(int port) {
		super(0, port);
		addListener(this);
	}

	@Override
	public void onServerStart() {
		System.out.println("UDP Test Server started.");
	}

	@Override
	public void onServerStop() {
		System.out.println("UDP Test Server stopped.");
	}

	@Override
	public void onConnection(Connection connection) {
		this.connection = connection;
		System.out.println("New client connected to UDP Test Server.");
	}

	@Override
	public void onDisconnect(Connection connection) {
		System.out.println("Client disconnected from UDP Test Server.");
	}

	@Override
	public void onReceived(Connection connection, Packet packet) {
		try {
		System.out.println("[Server] Receive: " + packet.getString());
		} catch (PacketException e) {
			e.printStackTrace();
		}
		Packet p = new Packet((short) 2);
		p.addString("Reply");
		connection.sendUdp(p);
	}
}
