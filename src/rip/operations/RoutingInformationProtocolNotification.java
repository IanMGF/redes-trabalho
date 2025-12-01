package rip.operations;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RoutingInformationProtocolNotification extends RoutingInformationProtocolOperation {
    private final short nodeAId;
    private final short nodeBId;
    private final short cost;
    public RoutingInformationProtocolNotification(short nodeAId, short nodeBId, short cost) {
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

    public static RoutingInformationProtocolNotification parse(String data) {
        Pattern pattern = Pattern.compile(RoutingInformationProtocolOperationType.NOTIFICATION + " ([0-9]+) ([0-9]+) ([0-9]+)");
        Matcher matcher = pattern.matcher(data);
        if (!matcher.matches()) {
            return null;
        }

        short nodeAId = Short.parseShort(matcher.group(1));
        short nodeBId = Short.parseShort(matcher.group(2));
        short cost = Short.parseShort(matcher.group(2));
        return new RoutingInformationProtocolNotification(nodeAId, nodeBId, cost);
    }

    @Override
    public String toString() {
        return RoutingInformationProtocolOperationType.NOTIFICATION + " " + getNodeAId() + " " + getNodeBId() + " " + getCost();
    }
}
