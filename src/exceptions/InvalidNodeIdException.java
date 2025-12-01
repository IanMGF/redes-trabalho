package exceptions;

public class InvalidNodeIdException extends Exception {
    private final int nodeId;
    public InvalidNodeIdException(int nodeId, String message) {
        super(message);
        this.nodeId = nodeId;
    }

    public int getNodeId() {
        return nodeId;
    }
}
