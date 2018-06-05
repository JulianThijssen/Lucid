package lucid.exceptions;

@SuppressWarnings("serial")
public class ConnectionException extends Exception {
    public ConnectionException(Exception e) {
        super(e);
    }

    public ConnectionException(String message) {
        super(message);
    }
}
