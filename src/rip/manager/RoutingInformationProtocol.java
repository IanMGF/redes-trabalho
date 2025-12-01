package rip.manager;

import exceptions.InvalidFormatException;
import exceptions.InvalidPortException;
import rip.operations.RoutingInformationProtocolOperation;
import rip.operations.RoutingInformationProtocolOperationType;
import up.UnicastProtocol;
import up.UnicastServiceInterface;
import up.UnicastServiceUserInterface;

import java.io.FileNotFoundException;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * @author Ian Marcos Gomes e Freitas
 * @author João Roberto de Moraes Neto
 *
 *
 */
public class RoutingInformationProtocol
    implements UnicastServiceUserInterface, RoutingProtocolManagementInterface {
    private final short UCSAPID = 0;
    private final int PORT = 520;
    private UnicastServiceInterface unicastInterface;
    private RoutingProtocolManagementServiceUserInterface ripServiceUserInterface;

    public RoutingInformationProtocol(
        RoutingProtocolManagementServiceUserInterface ripServiceUserInterface
    ) {
        try {
            UnicastServiceInterface unicastInterface = new UnicastProtocol(UCSAPID, PORT, this);
        } catch (IllegalArgumentException iae) {
            System.err.println("Erro : Conjunto ID e Porta não foram encontrados no arquivo de configuração do Unicast");
        } catch (FileNotFoundException fnfe) {
            System.err.println("Erro : Não foi encontrado arquivo de configuração do Unicast ('unicast.conf')");
        } catch (SocketException se) {
            System.err.printf("Erro : Não foi possível inicar atrelar socket à porta %s\n", PORT);
        } catch (UnknownHostException uhe) {
            System.err.printf("Erro : Endereço IP: '%s' contido no arquivo de configuração do Unicast é inválido\n", uhe);
        } catch (InvalidFormatException ife) {
            System.err.printf("Erro: Linha do arquivo de configuração Unicast não seguem o formato <ucsapid> <host> <porta>: '%s'\n", ife.getText());
        } catch (InvalidPortException ipe) {
            System.err.printf("Erro: Porta inválida encontrada no arquivo de configuração: %s. Portas válidas: 1025-65535\n", ipe.getPort());
        }

        this.ripServiceUserInterface = ripServiceUserInterface;
    }

    @Override
    public boolean getDistanceTable(short nodeId) {
        if (!isNodeValid(nodeId)) {
            return false;
        }

        unicastInterface.UPDataReq(
            nodeId,
            RoutingInformationProtocolOperationType.REQUEST.name()
        );

        return true;
    }

    @Override
    public boolean getLinkCost(short firstNodeId, short secondNodeId) {
        if (!isLinkValid(firstNodeId, secondNodeId)) {
            return false;
        }

        String packedData =
            RoutingInformationProtocolOperationType.GET.name() +
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
            RoutingInformationProtocolOperationType.SET.name() +
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
