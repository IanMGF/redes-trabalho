package rip.manager;

import exceptions.InvalidFormatException;
import exceptions.InvalidPortException;
import rip.operations.*;
import up.UnicastProtocol;
import up.UnicastServiceInterface;
import up.UnicastServiceUserInterface;

import java.io.FileNotFoundException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

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

    private RoutingInformationProtocolOperation latestOperation;
    private short latestNodeId;
    private Timer operationResponseTimeout;
    private Semaphore latestDataAccess;
    private int timeoutMilliseconds;

    public RoutingInformationProtocol(
        RoutingProtocolManagementServiceUserInterface ripServiceUserInterface, int timeoutMilliseconds
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
        this.timeoutMilliseconds = timeoutMilliseconds;
        this.latestDataAccess = new Semaphore(1);
    }

    public RoutingInformationProtocol(
        RoutingProtocolManagementServiceUserInterface ripServiceUserInterface
    ) {
        this(ripServiceUserInterface, 10_000);
    }

    @Override
    public boolean getDistanceTable(short nodeId) {
        if (!isNodeValid(nodeId)) {
            return false;
        }

        RoutingInformationProtocolOperation distanceTableRequest = new RoutingInformationProtocolRequest();
        executeOperationOnTimeout(nodeId, distanceTableRequest);

        return true;
    }

    @Override
    public boolean getLinkCost(short firstNodeId, short secondNodeId) {
        if (!isLinkValid(firstNodeId, secondNodeId)) {
            return false;
        }

        RoutingInformationProtocolOperation currOperation = new RoutingInformationProtocolGet(firstNodeId, secondNodeId);
        executeOperationOnTimeout(firstNodeId, currOperation);

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

        RoutingInformationProtocolSet currOperation = new RoutingInformationProtocolSet(firstNodeId, secondNodeId, (short) newLinkCost);
        executeOperationOnTimeout(firstNodeId, currOperation);

        return true;
    }

    private void executeOperationOnTimeout(short targetNodeId, RoutingInformationProtocolOperation operation) {
        try {
            latestDataAccess.acquire();
            latestOperation = operation;
            operationResponseTimeout.scheduleAtFixedRate(new TimerTask (){
                @Override
                public void run() { unicastInterface.UPDataReq(targetNodeId, operation.toString()); }
            }, 0, timeoutMilliseconds);
            latestDataAccess.release();
        } catch (InterruptedException e) {
            System.err.printf("Erro ao capturar semáforo latestDataAccess:\n%s\n", e);
        }
    }

    private void terminateOperationTimeout() {
        try {
            latestDataAccess.acquire();
            latestOperation = null;
            latestNodeId = -1;
            operationResponseTimeout.cancel();
            operationResponseTimeout = new Timer();
            latestDataAccess.release();
        } catch (InterruptedException e) {
            System.err.printf("Erro ao capturar semáforo latestDataAccess:\n%s\n", e);
        }
    }

    @Override
    public void UPDataInd(short Nodeid, String data) {
        RoutingInformationProtocolOperation receivedOperation = RoutingInformationProtocolOperationParser.parse(data);
        if (receivedOperation == null) return;

        if (
                latestOperation instanceof RoutingInformationProtocolGet getOperation
                && receivedOperation instanceof RoutingInformationProtocolNotification notifyOperation
                && getOperation.getNodeAId() == notifyOperation.getNodeAId()
                && getOperation.getNodeBId() == notifyOperation.getNodeBId()
        ) {
            terminateOperationTimeout();
            ripServiceUserInterface.linkCostIndication(notifyOperation.getNodeAId(), notifyOperation.getNodeBId(), notifyOperation.getCost());
        } else if (
                latestOperation instanceof RoutingInformationProtocolRequest reqOperation
                && receivedOperation instanceof RoutingInformationProtocolResponse responseOperation
                && latestNodeId == responseOperation.getNodeId()
        ) {
            terminateOperationTimeout();
            ripServiceUserInterface.distanceTableIndication(responseOperation.getNodeId(), responseOperation.getDistanceTable());
        }

        // TODO: Add listener for receiving operation of set link cost
    }

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
