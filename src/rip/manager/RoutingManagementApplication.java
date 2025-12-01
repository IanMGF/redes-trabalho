package rip;

public class RoutingManagementApplication
    implements RoutingProtocolManagementServiceUserInterface {

    @Override
    public void distanceTableIndication(short nodeId, int[][] distanceTable) {}

    @Override
    public void linkCostIndication(
        short firstNodeId,
        short secondNodeId,
        int linkCost
    ) {}
}
