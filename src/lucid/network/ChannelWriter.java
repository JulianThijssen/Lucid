package lucid.network;

import java.io.IOException;
import java.nio.ByteBuffer;

@FunctionalInterface
public interface ChannelWriter {
	public int write(ByteBuffer b) throws IOException;
}
