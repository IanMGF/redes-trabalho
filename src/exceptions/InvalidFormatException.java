package exceptions;

public class InvalidFormatException extends Exception {
    private final String text;
    public InvalidFormatException(String message, String text) {
        super(message);
        this.text = text;
    }

    public String getText() {
        return text;
    }
}
