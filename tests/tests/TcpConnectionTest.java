package tests;

import static org.junit.Assert.*;
import lucid.client.TcpConnection;
import lucid.exceptions.ConnectionException;
import lucid.exceptions.ServerStartException;
import lucid.network.ConnectionStatus;
import lucid.network.Packet;
import lucid.util.Log;
import lucid.util.LogLevel;
import tests.helper.TcpTestServer;

import org.junit.Test;

public class TcpConnectionTest {

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

        for (int i = 0; i < 150; i++) {
            Packet packet = new Packet((short) 1);
            packet.addString("Packet " + i);

            TcpConnection connection = new TcpConnection();
            connection.connect("127.0.0.1", 4444);
            connection.send(packet);
            //System.out.println(reply);
            assertTrue(connection.getConnectionStatus() == ConnectionStatus.CONNECTED);

            connection.close();

            try {
                Thread.sleep(10); // Retrying too fast doesn't work due to a single connection being allowed
            } catch(Exception e) {

            }
        }

        server.close();
    }
}
