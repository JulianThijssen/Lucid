package lucid.exceptions;

@SuppressWarnings("serial")
public class ChannelWriteException extends Exception {
	public ChannelWriteException(Exception e) {
		super(e);
	}
	
	public ChannelWriteException(String message) {
		super(message);
	}
}
