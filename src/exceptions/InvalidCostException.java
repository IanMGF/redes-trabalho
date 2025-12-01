package exceptions;

public class InvalidCostException extends Exception {
    private final int cost;

    public InvalidCostException(int cost, String message) {
        super(message);
        this.cost = cost;
    }

    public int getCost() {
        return cost;
    }
}
