package rip.operations;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RoutingInformationProtocolIndication extends RoutingInformationProtocolOperation {
    private final short nodeId;
    private final int[] distanceVector;
    public RoutingInformationProtocolIndication(short nodeId, int[] distanceVector) {
        this.nodeId = nodeId;
        this.distanceVector = distanceVector;
    }

    public short getNodeId() {
        return nodeId;
    }

    public int[] getDistanceVector() {
        return distanceVector;
    }

    public static RoutingInformationProtocolIndication parse(String data) {
        Pattern pattern = Pattern.compile(RoutingInformationProtocolOperationType.INDICATION + " ([0-9]+) ([0-9 :]+)");
        Matcher matcher = pattern.matcher(data);
        if (!matcher.matches()) {
            return null;
        }

        short nodeAId = Short.parseShort(matcher.group(1));
        String distanceTableStr = matcher.group(2);
        int[] distanceTable = DistanceVectorParser.parse(distanceTableStr);
        if (distanceTable == null) return null;
        return new RoutingInformationProtocolIndication(nodeAId, distanceTable);
    }

    @Override
    public String toString() {
        String typeIndicator = RoutingInformationProtocolOperationType.INDICATION.toString();
        String nodeIdStr = String.valueOf(getNodeId());
        List<String> distancesStr = new ArrayList<>(distanceVector.length);
        for (int distance: distanceVector) {
            distancesStr.add(String.valueOf(distance));
        }
        String distanceVectorStr = String.join(":", distancesStr);

        return typeIndicator + " " +  nodeIdStr + " " + distanceVectorStr;
    }
}
