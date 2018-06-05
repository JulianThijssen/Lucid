package tests;

import static org.junit.Assert.*;

import java.util.Queue;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.Test;

import lucid.client.NetworkListener;
import lucid.client.TcpConnection;
import lucid.exceptions.ConnectionException;
import lucid.exceptions.ServerStartException;
import lucid.network.Packet;
import lucid.util.Log;
import lucid.util.LogLevel;
import tests.helper.TcpEchoServer;

public class PacketTransportTest implements NetworkListener {
	private Queue<Packet> sentPackets = new LinkedBlockingQueue<Packet>();
	private Queue<Packet> receivedPackets = new LinkedBlockingQueue<Packet>();

	@Test
	public void test() throws ConnectionException {
		Log.listenLevel = LogLevel.ALL;
		
		System.out.println("Starting PacketTransportTest...");
		TcpEchoServer server = new TcpEchoServer(4444);

		try {
			server.start();
		} catch (ServerStartException e) {
			System.out.println("Server failed to start");
		}
		
		TcpConnection connection = new TcpConnection();
		TcpConnection.sid++;
		connection.addListener(this);
		connection.connect("127.0.0.1", 4444);

		for (int i = 0; i < 15000; i++) {
			Packet packet = generateRandomPacket();
			sentPackets.add(packet);

			connection.send(packet);
		}
		
		while (sentPackets.size() != receivedPackets.size()) {
			try {
				Thread.sleep(100);
				//System.out.println(receivedPackets.size());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		int size = receivedPackets.size();
		
		for (int i = 0; i < size; i++) {
			//System.out.println(i + " " + size);
			Packet sentPacket = sentPackets.poll();
			Packet receivedPacket = receivedPackets.poll();
			
			byte[] a = sentPacket.getData();
			byte[] b = receivedPacket.getData();
			for (int j = 0; j < a.length; j++) {
				if (a[j] != b[j]) {
					fail("Received packet doesn't match sent packet");
				}
			}
			
			//System.out.println("Packet: " + i + " - " + receivedPacket.toString());
		}
		
		connection.close();
		
		server.close();
	}
	
	private Packet generateRandomPacket() {
		Random random = new Random();
		
		Packet packet = new Packet((short) random.nextInt(30000));
		
		for (int i = 0; i < random.nextInt(30) + 5; i++) {
			int toAdd = random.nextInt(8);
			
			switch(toAdd) {
			case 0: packet.addBoolean(false); break;
			case 1: packet.addBoolean(true); break;
			case 2: packet.addByte((byte) random.nextInt(255)); break;
			case 3: packet.addShort((short) random.nextInt(65535)); break;
			case 4: packet.addInt(random.nextInt()); break;
			case 5: packet.addFloat(random.nextFloat()); break;
			case 6: packet.addLong(random.nextLong()); break;
			case 7: packet.addString("Some string " + random.nextInt()); break;
			}
		}
		
		return packet;
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
		receivedPackets.add(packet);
	}
}
