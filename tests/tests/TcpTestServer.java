package tests;
import lucid.network.Connection;
import lucid.network.Packet;
import lucid.network.Server;
import lucid.network.ServerListener;

public class TcpTestServer extends Server implements ServerListener {

	public TcpTestServer(int port) {
		super(port, 0);
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
		System.out.println("Connection");
	}

	@Override
	public void onDisconnect(Connection connection) {
		System.out.println("Disconnection");
	}

	@Override
	public void onReceived(Connection connection, Packet packet) {
		System.out.println("Receive: " + packet.getString());
		Packet p = new Packet(2);
		p.addString("Reply");
		connection.sendTcp(p);
	}

}
