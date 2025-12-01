package exceptions;

public class InvalidPortException extends Exception {
    private final int port;
    public InvalidPortException(int port, String message) {
        super(message);
        this.port = port;
    }

    public int getPort() {
        return port;
    }
}
