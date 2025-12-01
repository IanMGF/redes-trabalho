package rip.manager;

import rip.RoutingInformationProtocolOperation;
import up.UnicastServiceInterface;
import up.UnicastServiceUserInterface;

/**
 * @author Ian Marcos Gomes e Freitas
 * @author João Roberto de Moraes Neto
 *
 *
 */

public class RoutingInformationProtocol
    implements UnicastServiceUserInterface, RoutingProtocolManagementInterface {

    private UnicastServiceInterface unicastInterface;
    private RoutingProtocolManagementServiceUserInterface ripServiceUserInterface;

    public RoutingInformationProtocol(
        UnicastServiceInterface unicastInterface,
        RoutingProtocolManagementServiceUserInterface ripServiceUserInterface
    ) {
        this.unicastInterface = unicastInterface;
        this.ripServiceUserInterface = ripServiceUserInterface;
    }

    @Override
    public boolean getDistanceTable(short nodeId) {
        if (!isNodeValid(nodeId)) {
            return false;
        }

        unicastInterface.UPDataReq(
            nodeId,
            RoutingInformationProtocolOperation.RIPRQT.name()
        );

        return true;
    }

    @Override
    public boolean getLinkCost(short firstNodeId, short secondNodeId) {
        if (!isLinkValid(firstNodeId, secondNodeId)) {
            return false;
        }

        String packedData =
            RoutingInformationProtocolOperation.RIPGET.name() +
            " " +
            firstNodeId +
            " " +
            secondNodeId;

        unicastInterface.UPDataReq(firstNodeId, packedData);

        return true;
    }

    @Override
    public boolean setLinkCost(
        short firstNodeId,
        short secondNodeId,
        int newLinkCost
    ) {
        if (
            !(isLinkValid(firstNodeId, secondNodeId) &&
                isCostValid(newLinkCost))
        ) {
            return false;
        }

        String packedData =
            RoutingInformationProtocolOperation.RIPSET.name() +
            " " +
            firstNodeId +
            " " +
            secondNodeId +
            " " +
            newLinkCost;

        unicastInterface.UPDataReq(firstNodeId, packedData);

        return true;
    }

    @Override
    public void UPDataInd(short Nodeid, String data) {}

    //TODO : finish validation methods
    private boolean isNodeValid(short nodeId) {
        return true;
    }

    private boolean isLinkValid(short firstNodeId, short secondNodeId) {
        return true;
    }

    private boolean isCostValid(int linkCost) {
        if ((linkCost > 0 && linkCost < 16) || linkCost == -1) {
            return true;
        } else {
            return false;
        }
    }
}
