package lucid.client;

import lucid.network.Packet;

public interface NetworkListener {
	
	public void connected();
	
	public void disconnected();
	
	public void received(Packet packet);
}
