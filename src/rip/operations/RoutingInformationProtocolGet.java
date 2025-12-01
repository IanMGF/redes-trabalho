package rip.operations;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RoutingInformationProtocolGet extends RoutingInformationProtocolOperation {
    private final short nodeAId;
    private final short nodeBId;
    public RoutingInformationProtocolGet(short nodeAId, short nodeBId) {
        this.nodeAId = nodeAId;
        this.nodeBId = nodeBId;
    }

    public short getNodeAId() {
        return nodeAId;
    }

    public short getNodeBId() {
        return nodeBId;
    }

    public static RoutingInformationProtocolGet parse(String data) {
        Pattern pattern = Pattern.compile(RoutingInformationProtocolOperationType.GET + " ([0-9]+) ([0-9]+)");
        Matcher matcher = pattern.matcher(data);
        if (!matcher.matches()) {
            return null;
        }

        short nodeAId = Short.parseShort(matcher.group(1));
        short nodeBId = Short.parseShort(matcher.group(2));
        return new RoutingInformationProtocolGet(nodeAId, nodeBId);
    }

    @Override
    public String toString() {
        return RoutingInformationProtocolOperationType.GET + " " + getNodeAId() + " " +  getNodeBId();
    }
}
