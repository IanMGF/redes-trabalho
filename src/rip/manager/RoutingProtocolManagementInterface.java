package rip.manager;

public interface RoutingProtocolManagementInterface {
    boolean getDistanceTable(short nodeId);
    boolean getLinkCost(short firstNodeId, short secondNodeId);
    boolean setLinkCost(short firstNodeId, short secondNodeId, int newLinkCost);
}
