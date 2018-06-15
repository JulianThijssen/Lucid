package lucid.network;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;

import lucid.exceptions.ChannelReadException;
import lucid.exceptions.ChannelWriteException;

public class TcpChannel implements NetworkChannel {
    private SocketChannel channel;

    //private PacketBuffer packetBuffer = new PacketBuffer();
    private PacketInputBuffer packetInputBuffer = new PacketInputBuffer();
    private PacketOutputBuffer packetOutputBuffer = new PacketOutputBuffer();

    public TcpChannel(SocketChannel channel) {
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

    @Override
    public boolean hasPackets() {
        return packetInputBuffer.hasPackets();
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
        packetOutputBuffer.write(b -> { return channel.write(b); });
    }

    @Override
    public void read() throws ChannelReadException {
        try {
            SocketAddress address = getRemoteAddress();

            packetInputBuffer.read(address, b -> { return channel.read(b); });
        } catch (IOException e) {
            throw new ChannelReadException("Failed to get socket address.");
        }
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }
}
