package lucid.network;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;

import lucid.exceptions.ChannelReadException;
import lucid.exceptions.ChannelWriteException;

public interface NetworkChannel {
    public void setBlocking(boolean blocking) throws IOException;
    public boolean isConnected();
    public SocketAddress getRemoteAddress() throws ClosedChannelException, IOException;

    public void send(Packet packet);
    public Packet receive();

    public void write() throws ChannelWriteException;
    public void read() throws ChannelReadException;

    public void close() throws IOException;
}
