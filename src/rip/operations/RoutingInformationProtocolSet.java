package rip.operations;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RoutingInformationProtocolSet extends RoutingInformationProtocolOperation {
    private final short nodeAId;
    private final short nodeBId;
    private final short cost;
    private boolean hasBeenInverted = false;
    public RoutingInformationProtocolSet(short nodeAId, short nodeBId, short cost) {
        this.nodeAId = nodeAId;
        this.nodeBId = nodeBId;
        this.cost = cost;
    }

    public short getNodeAId() {
        return nodeAId;
    }

    public short getNodeBId() {
        return nodeBId;
    }

    public short getCost() {
        return cost;
    }

    /**
     * Returns the inverted version of the Set operation (i.e. (A, B, cost) -> (B, A, cost))
     * If the operation has already been reverted, returns null instead
     *
     * @return Inverted operation, or null if the current operation already is an inversion
     */
    public RoutingInformationProtocolSet getInvertedOrNull() {
        if (hasBeenInverted) {
            return null;
        } else {
            RoutingInformationProtocolSet invertedOp = new RoutingInformationProtocolSet(nodeBId, nodeAId, cost);
            invertedOp.hasBeenInverted = true;
            return invertedOp;
        }
    }

    public static RoutingInformationProtocolSet parse(String data) {
        Pattern pattern = Pattern.compile(RoutingInformationProtocolOperationType.SET + " ([0-9]+) ([0-9]+) ([0-9]+)");
        Matcher matcher = pattern.matcher(data);
        if (!matcher.matches()) {
            return null;
        }

        short nodeAId = Short.parseShort(matcher.group(1));
        short nodeBId = Short.parseShort(matcher.group(2));
        short cost = Short.parseShort(matcher.group(2));
        return new RoutingInformationProtocolSet(nodeAId, nodeBId, cost);
    }

    @Override
    public String toString() {
        return RoutingInformationProtocolOperationType.SET + " " + getNodeAId() + " " + getNodeBId() + " " + getCost();
    }
}
