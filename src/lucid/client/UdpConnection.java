package lucid.client;

import static lucid.network.ConnectionStatus.CONNECTED;
import static lucid.network.ConnectionStatus.CONNECTING;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import lucid.network.Packet;
import lucid.network.UdpChannel;
import lucid.util.Log;
import lucid.util.LogLevel;
import lucid.util.Scheduler;

public class UdpConnection extends ClientConnection {
    private class Handshake {
        private final Packet SYN;
        private final Packet ACK;
        
        private Scheduler retryScheduler = null;
        
        public Handshake(long unique) {
            SYN = new Packet((short) 0);
            SYN.addLong(unique);
            ACK = new Packet((short) 1);
            
            Runnable retryTask = new Runnable() {
                @Override
                public void run() {
                    sendSyn();
                }
            };
            retryScheduler = new Scheduler(retryTask);
        }
        
        public void init() {
            retryScheduler.executeAtFixedRate(500);
        }
        
        public void complete() {
            retryScheduler.stop();
            // Acknowledge SYN-ACK
            sendAck();
        }
        
        private void sendSyn() {
            send(SYN);
        }
        
        public void sendAck() {
            send(ACK);
        }
    }

    private InetSocketAddress address = null;
   
    private Handshake handshake;
    
    public UdpConnection() {        
        handshake = new Handshake(unique);
    }
    
    public boolean connect(String host, int port) {
        address = new InetSocketAddress(host, port);

        try {
            DatagramChannel datagramChannel = DatagramChannel.open();
            datagramChannel.connect(address);
            datagramChannel.configureBlocking(false);

            channel = new UdpChannel(datagramChannel);
            
            selector = Selector.open();
            SelectionKey key = datagramChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            key.attach(this);

            setConnectionStatus(CONNECTING);
            start();
            
            // Initiate the handshake
            handshake.init();
        } catch(IOException e) {
            e.printStackTrace();
            close();
            Log.debug(LogLevel.CLIENT, String.format("Failed to connect to host: %s at port: %d", host, port));
            return false;
        }

        return true;
    }

    @Override
    public void onPacketReceived(Packet packet) {
        if (packet.getType() == 0) {
            if (getConnectionStatus() == CONNECTING) {
                Log.debug(LogLevel.CLIENT, "Received SYN-ACK packet");
                handshake.complete();
                notifyConnected();
                Log.debug(LogLevel.CLIENT, String.format("Successfully connected to host: " + address));
            }
            else if (getConnectionStatus() == CONNECTED) {
                // Server didn't receive ACK packet, so re-send it
                handshake.complete();
            }
        }
        else {
            notifyReceived(packet);
        }
    }
}
