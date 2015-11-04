package tests;

import lucid.network.Connection;
import lucid.network.Packet;
import lucid.network.Server;
import lucid.network.ServerListener;

public class SpamUdpTestServer extends Server implements ServerListener {
	public Connection connection;
	private Packet packet;
	private Sender sender = new Sender();
	
	public SpamUdpTestServer(int tcpPort, int udpPort) {
		super(tcpPort, udpPort);
		addListener(this);
		packet = new Packet(0);
		packet.addString("UDP Test Packet");
	}

	public void begin() {
		sender.start();
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
		Packet p = new Packet(0);
		p.addString("Reply");
		connection.sendUdp(p);
	}
	
	private class Sender implements Runnable {
		public void start() {
			new Thread(this).start();
		}
		
		@Override
		public void run() {
			while (true) {
				if (connection != null) {
					System.out.println("Sent!");
					connection.sendUdp(packet);
				}
				
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
