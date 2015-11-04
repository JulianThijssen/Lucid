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
		SpamUdpTestServer server = new SpamUdpTestServer(4444, 4445);
		
		server.start();
		
		System.out.println("Preconnect");
		TcpConnection tcp = new TcpConnection();
		tcp.addListener(this);
		tcp.connect("127.0.0.1", 4444);
		UdpConnection udp = new UdpConnection();
		udp.addListener(this);
		udp.connect("127.0.0.1", 4445);
		System.out.println("Postconnect");
		//udp.send(packet);
		server.begin();
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
