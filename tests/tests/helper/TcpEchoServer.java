package tests.helper;

import lucid.network.Connection;
import lucid.network.Packet;
import lucid.network.Server;
import lucid.network.ServerListener;

public class TcpEchoServer extends Server implements ServerListener {

    public TcpEchoServer(int port) {
        super(port, 0);
        addListener(this);
    }

    @Override
    public void onServerStart() {
        System.out.println("Started TcpTestServer");
    }

    @Override
    public void onServerStop() {
        System.out.println("Stopped TcpTestServer");
    }

    @Override
    public void onConnection(Connection connection) {
        System.out.println("Connection on TcpTestServer " + connection);
    }

    @Override
    public void onDisconnect(Connection connection) {
        System.out.println("Disconnection on TcpTestServer " + connection);
    }
    int b = 0;
    @Override
    public void onReceived(Connection connection, Packet packet) {
        //System.out.println("SENDING: " + (b++) + " " + packet.getType());
        connection.sendTcp(packet);
    }
}
