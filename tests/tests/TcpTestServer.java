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
		System.out.println("Started TcpTestServer");
	}

	@Override
	public void onServerStop() {
		System.out.println("Stopped TcpTestServer");
	}

	@Override
	public void onConnection(Connection connection) {
		System.out.println("Connection on TcpTestServer " + connection);
	}

	@Override
	public void onDisconnect(Connection connection) {
		System.out.println("Disconnection on TcpTestServer " + connection);
	}

	@Override
	public void onReceived(Connection connection, Packet packet) {
		System.out.println("Receive on TcpTestServer: " + packet.getString());
		Packet p = new Packet(2);
		p.addString("Reply");
		connection.sendTcp(p);
	}

}
