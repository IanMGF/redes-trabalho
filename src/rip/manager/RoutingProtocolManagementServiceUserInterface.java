package rip;

public interface RoutingProtocolManagementServiceUserInterface {
    void distanceTableIndication(short nodeId, int[][] distanceTable);
    void linkCostIndication(
        short firstNodeId,
        short secondNodeId,
        int linkCost
    );
}
