public class InvalidUnicastDataUnitException extends RuntimeException {
    public String data;
    public InvalidUnicastDataUnitException(String data) {
        super("Bad protocol data unit: \"%s\"".formatted(data));
        this.data = data;
    }
}
