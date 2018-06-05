package lucid.exceptions;

@SuppressWarnings("serial")
public class ChannelReadException extends Exception {
    public ChannelReadException(Exception e) {
        super(e);
    }

    public ChannelReadException(String message) {
        super(message);
    }
}
