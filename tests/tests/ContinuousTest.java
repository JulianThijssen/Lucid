package tests;

import lucid.client.NetworkListener;
import lucid.client.TcpConnection;
import lucid.client.UdpConnection;
import lucid.network.Packet;
import lucid.util.Log;
import lucid.util.LogLevel;

public class ContinuousTest implements NetworkListener {
	long time = System.currentTimeMillis();
	
	public static void main(String[] args) {
		new ContinuousTest();
	}
	
	public ContinuousTest() {
		Log.listenLevel = LogLevel.ALL;
		SpamUdpTestServer server = new SpamUdpTestServer(4445);
		TcpTestServer tcpServer = new TcpTestServer(4444);
		
		server.start();
		tcpServer.start();

		UdpConnection udp = new UdpConnection();
		udp.addListener(this);
		System.out.println("Preconnect");
		udp.connect("127.0.0.1", 4445);
		TcpConnection tcp = new TcpConnection();
		tcp.addListener(this);
		tcp.connect("127.0.0.1", 4444);
		System.out.println("Postconnect");
		//udp.send(packet);
		server.tick();
		while (true) {
			//System.out.println("Trying to receive...");
			try {
				Packet packet = new Packet(0);
				tcp.send(packet);
				Thread.sleep(10);
			} catch(Exception e) {
				
			}
		}
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
		System.out.println("Time: " + (System.currentTimeMillis() - time));
		time = System.currentTimeMillis();
		
		//System.out.println("Received!");
	}
}
