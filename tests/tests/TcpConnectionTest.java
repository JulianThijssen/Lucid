package tests;

import static org.junit.Assert.*;
import lucid.client.TcpConnection;
import lucid.network.Packet;
import lucid.util.Log;
import lucid.util.LogLevel;

import org.junit.Test;

public class TcpConnectionTest {

	@Test
	public void testTcpConnection() {
		Log.listenLevel = LogLevel.NONE;
		
		System.out.println("Starting TcpConnectionTest...");
		TcpTestServer server = new TcpTestServer(4444);

		server.start();
		
		
		
		for (int i = 0; i < 150; i++) {
			Packet packet = new Packet(1);
			packet.addString("Packet " + i);
			
			TcpConnection connection = new TcpConnection();
			TcpConnection.sid++;
			connection.connect("127.0.0.1", 4444);
			connection.send(packet);
			//System.out.println(reply);
			assertTrue(connection.isConnected());

			connection.close();

			try {
				Thread.sleep(10); // Retrying too fast doesn't work due to a single connection being allowed
			} catch(Exception e) {
				
			}
		}
		
		server.close();
	}
}
