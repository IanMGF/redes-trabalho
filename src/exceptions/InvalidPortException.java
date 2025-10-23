package exceptions;

public class InvalidPortException extends RuntimeException {
    private final short port;
    public InvalidPortException(short port, String message) {
        super(message);
        this.port = port;
    }

    public short getPort() {
        return port;
    }
}
