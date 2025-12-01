package rip.operations;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class RoutingInformationProtocolResponse extends RoutingInformationProtocolOperation {
    private final short nodeId;
    private final int[][] distanceTable;
    public RoutingInformationProtocolResponse(short nodeId, int[][] distanceTable) {
        this.nodeId = nodeId;
        this.distanceTable = distanceTable;
    }

    public short getNodeId() {
        return nodeId;
    }

    public int[][] getDistanceTable() {
        return distanceTable;
    }

    public static RoutingInformationProtocolResponse parse(String data) {
        Pattern pattern = Pattern.compile(RoutingInformationProtocolOperationType.RESPONSE + " ([0-9]+) ([0-9 :]+)");
        Matcher matcher = pattern.matcher(data);
        if (!matcher.matches()) {
            return null;
        }

        short nodeAId = Short.parseShort(matcher.group(1));
        String distanceTableStr = matcher.group(2);
        int[][] distanceTable = DistanceTableParser.parse(distanceTableStr);
        if (distanceTable == null) return null;
        return new RoutingInformationProtocolResponse(nodeAId, distanceTable);
    }

    @Override
    public String toString() {
        String typeInd = RoutingInformationProtocolOperationType.RESPONSE.toString();
        String nodeIdStr = String.valueOf(nodeId);
        String[] distanceVectorStrings =  new String[distanceTable.length];
        for (int i = 0; i < distanceTable.length; i++) {
            List<String> distancesStr = Stream.of(distanceTable[i]).map(Object::toString).toList();
            String distanceVectorStr = String.join(":", distancesStr);
            distanceVectorStrings[i] = distanceVectorStr;
        }
        String formattedDistanceVector = String.join(" ", distanceVectorStrings);
        return typeInd + " " + nodeIdStr + " " +  formattedDistanceVector;
    }
}
