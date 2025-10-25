package exceptions;

public class InvalidPortException extends RuntimeException {
    private final int port;
    public InvalidPortException(int port, String message) {
        super(message);
        this.port = port;
    }

    public int getPort() {
        return port;
    }
}
