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
		System.out.println("Start");
	}

	@Override
	public void onServerStop() {
		System.out.println("Stop");
	}

	@Override
	public void onConnection(Connection connection) {
		this.connection = connection;
		System.out.println("Connection");
	}

	@Override
	public void onDisconnect(Connection connection) {
		System.out.println("Disconnection");
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