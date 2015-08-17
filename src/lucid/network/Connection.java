package lucid.network;

import java.net.InetSocketAddress;

public class Connection {
	public TcpConnection tcp;
	public UdpConnection udp;
	
	public InetSocketAddress address = null;
	
	public void sendTcp(Packet packet) {
		if (tcp != null) {
			tcp.send(packet);
		}
	}
	
	public void sendUdp(Packet packet) {
		if (udp != null) {
			udp.send(packet);
		}
	}
	
	public void close() {
		if (tcp != null) {
			tcp.close();
		}
		if (udp != null) {
			udp.close();
		}
	}
}
