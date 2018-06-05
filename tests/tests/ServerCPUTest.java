package tests;

import org.junit.Test;

import lucid.exceptions.ConnectionException;
import lucid.exceptions.ServerStartException;
import lucid.util.Log;
import lucid.util.LogLevel;
import tests.helper.TcpTestServer;

public class ServerCPUTest {
	@Test
	public void testTcpConnection() throws ConnectionException {
		Log.listenLevel = LogLevel.NONE;
		
		System.out.println("Starting TcpConnectionTest...");
		TcpTestServer server = new TcpTestServer(4444);

		try {
			server.start();
		} catch (ServerStartException e) {
			System.out.println("Server failed to start");
		}
		
		while (server.isRunning()) {
			System.out.println("Beep");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		server.close();
	}
}
