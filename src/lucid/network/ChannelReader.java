package lucid.network;

import java.io.IOException;
import java.nio.ByteBuffer;

@FunctionalInterface
public interface ChannelReader {
	public int read(ByteBuffer b) throws IOException;
}
