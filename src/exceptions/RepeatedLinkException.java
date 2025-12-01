package exceptions;

public class RepeatedLinkException extends Exception {
    private short nodeAId;
    private short nodeBId;
    public RepeatedLinkException(short nodeAId, short nodeBId, String message) {
        super(message);
    }

    public short getNodeAId() {
        return nodeAId;
    }

    public short getNodeBId() {
        return nodeBId;
    }
}
