package lucid.exceptions;

// TODO Make unchecked exception after library is ready
// This forces me to catch all the exceptions, but not the user
@SuppressWarnings("serial")
public class PacketException extends Exception {
    public PacketException(String message) {
        super(message);
    }
}
