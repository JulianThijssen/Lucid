package lucid.exceptions;

@SuppressWarnings("serial")
public class DatabaseException extends Exception {
    public DatabaseException(String message) {
        super(message);
    }
}
