package tests;

import lucid.network.Connection;
import lucid.network.Packet;
import lucid.network.Server;
import lucid.network.ServerListener;

public class SpamUdpTestServer extends Server implements ServerListener {
	public Connection connection;
	private Packet packet;
	
	public SpamUdpTestServer(int port) {
		super(0, port);
		addListener(this);
		packet = new Packet(1);
		packet.addString("UDP Test Packet");
	}

	public void tick() {
		while (true) {
			if (connection != null) {
				System.out.println("Sent!");
				connection.sendUdp(packet);
			}
			
			try {
				Thread.sleep(60);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
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
		System.out.println("[Server] Receive: " + packet.getString());
		Packet p = new Packet(2);
		p.addString("Reply");
		connection.sendUdp(p);
	}
}
