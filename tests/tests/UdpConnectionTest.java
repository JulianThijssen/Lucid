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
    
    private UdpConnection udp;

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
        
        udp = new UdpConnection();
        udp.addListener(this);
        udp.connect("127.0.0.1", 4445);
        
        while (!received) {
            System.out.println("Trying to receive...");

            try {
                Thread.sleep(500);
            } catch(Exception e) {

            }
        }
        assertTrue(true);
    }

    @Override
    public void connected() {
        Packet packet = new Packet((short) 2);
        packet.addString("UDP Test Packet");

        udp.send(packet);
    }

    @Override
    public void disconnected() {
        

    }

    @Override
    public void received(Packet packet) {
        received = true;
        System.out.println("[Client] UDP receive: " + packet.toString());
    }
}
