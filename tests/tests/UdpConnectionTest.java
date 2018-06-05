package tests;

import static org.junit.Assert.*;
import lucid.client.NetworkListener;
import lucid.client.UdpConnection;
import lucid.exceptions.ServerStartException;
import lucid.network.Packet;
import lucid.util.Log;
import lucid.util.LogLevel;
import tests.helper.UdpTestServer;

import org.junit.Test;

public class UdpConnectionTest implements NetworkListener {
	private boolean received = false;

	@Test
	public void testUdpConnection() {
		System.out.println("Starting UdpConnectionTest...");
		UdpTestServer server = new UdpTestServer(4445);
		
		Log.listenLevel = LogLevel.ALL;
		
		try {
			server.start();
		} catch (ServerStartException e) {
			System.out.println("Server failed to start");
		}
		
		Packet packet = new Packet((short) 1);
		packet.addString("UDP Test Packet");
		
		UdpConnection udp = new UdpConnection();
		udp.addListener(this);
		boolean connected = udp.connect("127.0.0.1", 4445);
		if (!connected) {
			System.out.println("Failed to connect to server via UDP");
			return;
		}
		
		udp.send(packet);
		
		while (!received) {
			System.out.println("Trying to receive...");
			
			try {
				Thread.sleep(100);
			} catch(Exception e) {
				
			}
		}
		
		assertTrue(true);
	}

	@Override
	public void connected() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void disconnected() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void received(Packet packet) {
		received = true;
		System.out.println("[Client] UDP receive: " + packet.toString());
	}
}
