package lucid.network;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;

import lucid.exceptions.ChannelReadException;
import lucid.exceptions.ChannelWriteException;

public class UdpChannel implements NetworkChannel {
    private DatagramChannel channel;

    private PacketInputBuffer packetInputBuffer = new PacketInputBuffer();
    private PacketOutputBuffer packetOutputBuffer = new PacketOutputBuffer();

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
        return packetOutputBuffer.hasUnsentData();
    }

    public boolean hasPackets() {
        return packetInputBuffer.hasPackets();
    }

    public void connect(SocketAddress address) throws IOException {
        channel.connect(address);
    }

    @Override
    public void send(Packet packet) {
        packetOutputBuffer.send(packet);
    }

    @Override
    public Packet receive() {
        return packetInputBuffer.receive();
    }

    @Override
    public void write() throws ChannelWriteException {
        if (packetOutputBuffer.hasUnsentData()) {
            packetOutputBuffer.write(b -> { return channel.write(b); });
        }
    }

    @Override
    public void read() throws ChannelReadException {
        try {
            SocketAddress address = getRemoteAddress();

            int readBytes = packetInputBuffer.read(address, b -> { return channel.read(b); });
        } catch (IOException e) {
            throw new ChannelReadException("Failed to get socket address.");
        }
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }
}
