package lucid.network;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;

import lucid.exceptions.ChannelReadException;
import lucid.exceptions.ChannelWriteException;

public class UdpChannel implements NetworkChannel {
    private DatagramChannel channel;

    private PacketBuffer packetBuffer = new PacketBuffer();

    public UdpChannel(DatagramChannel channel) {
        this.channel = channel;
    }

    @Override
    public void setBlocking(boolean blocking) throws IOException {
        channel.configureBlocking(blocking);
    }

    @Override
    public boolean isConnected() {
        return channel.isConnected();
    }

    @Override
    public SocketAddress getRemoteAddress() throws ClosedChannelException, IOException {
        return channel.getRemoteAddress();
    }

    public boolean hasUnsentData() {
        return packetBuffer.hasUnsentData();
    }

    public boolean hasPackets() {
        return packetBuffer.hasPackets();
    }

    public void connect(SocketAddress address) throws IOException {
        channel.connect(address);
    }

    @Override
    public void send(Packet packet) {
        packetBuffer.send(packet);
    }

    @Override
    public Packet receive() {
        return packetBuffer.receive();
    }

    @Override
    public void write() throws ChannelWriteException {
        if (packetBuffer.hasUnsentData()) {
            packetBuffer.write(b -> { return channel.write(b); });
        }
    }

    @Override
    public void read() throws ChannelReadException {
        try {
            SocketAddress address = getRemoteAddress();

            int readBytes = packetBuffer.read(address, b -> { return channel.read(b); });
        } catch (IOException e) {
            throw new ChannelReadException("Failed to get socket address.");
        }
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }
}
