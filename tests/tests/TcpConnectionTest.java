package tests;

import static org.junit.Assert.*;
import lucid.client.TcpConnection;
import lucid.network.Packet;

import org.junit.Test;

public class TcpConnectionTest {

	@Test
	public void testTcpConnection() {
		System.out.println("Starting TcpConnectionTest...");
		TcpTestServer server = new TcpTestServer(4444);

		server.start();
		
		Packet packet = new Packet(1);
		packet.addString("TCP Test Packet");
		
		for (int i = 0; i < 150; i++) {
			TcpConnection connection = new TcpConnection();
			TcpConnection.sid++;
			connection.connect("127.0.0.1", 4444);
			connection.send(packet);
			//System.out.println(reply);
			assertTrue(connection.isConnected());
			
			try {
				Thread.sleep(1);
			} catch(Exception e) {
				
			}
		}
		
		server.close();
	}
}
