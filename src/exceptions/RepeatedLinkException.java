package exceptions;

public class RepeatedLinkException extends Exception {
    private final short nodeAId;
    private final short nodeBId;
    public RepeatedLinkException(short nodeAId, short nodeBId, String message) {
        super(message);
        this.nodeAId = nodeAId;
        this.nodeBId = nodeBId;
    }

    public short getNodeAId() {
        return nodeAId;
    }

    public short getNodeBId() {
        return nodeBId;
    }
}
