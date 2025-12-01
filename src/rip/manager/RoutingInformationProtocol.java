package rip.manager;

import exceptions.*;
import rip.RoutingInformationConfiguration;
import rip.operations.*;
import up.UnicastProtocol;
import up.UnicastServiceInterface;
import up.UnicastServiceUserInterface;

import java.io.File;
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
    private RoutingInformationConfiguration networkTopology;
    private RoutingInformationProtocolOperation latestOperation;
    private short latestNodeId;
    private Timer operationResponseTimeout;
    private Semaphore latestDataAccess;
    private int timeoutMilliseconds;

    public RoutingInformationProtocol(
        RoutingProtocolManagementServiceUserInterface ripServiceUserInterface, int timeoutMilliseconds
    ) {
        File ripConfigurationFile = new File("rip.conf");
        networkTopology = null;
        try {
            networkTopology = RoutingInformationConfiguration.loadFromFile(ripConfigurationFile);
        } catch (FileNotFoundException fnfe) {
            System.err.println("Erro : Não foi encontrado arquivo de configuração do Routing Information Protocol ('rip.conf')");
        } catch (InvalidFormatException ife) {
            System.err.printf("Erro: Linha do arquivo de configuração Routing Information Protocol não seguem o formato <RIPNode_1> <RIPNode_2> <custo>: '%s'\n", ife.getText());
        } catch (NonIncrementalIdsException niie) {
            System.err.println("Erro: Ids dos nós no arquivo de configuração do Routing Information Protocol('rip.conf') não estão em uma sequência incremental");
        } catch (InvalidNodeIdException inie) {
            System.err.printf("Erro: Id[%d] do(s) nó(s) no arquivo de configuração do Routing Information Protocol ('rip.conf') não é um número inteiro entre 1 e 15\n", inie.getNodeId());
        } catch (InvalidCostException ice) {
            System.err.printf("Erro: Custo %d presente no arquivo de configuração do Routing Information Protocol ('rip.conf') não é um número inteiro entre 1 e 15\n", ice.getCost());
        } catch (RepeatedLinkException rle) {
            System.err.printf("Erro: Par não-ordenado de IDs ([%d], [%d]) encontrado mais de uma vez no arquivo de configuração do Routing Information Protocol('rip.conf')\n", rle.getNodeAId(), rle.getNodeBId());
        }

        if (networkTopology == null) {
            return;
        }

        unicastInterface = null;
        try {
            unicastInterface = new UnicastProtocol(UCSAPID, PORT, this);
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

        if(unicastInterface == null) {
            return;
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

    /**
     * Sends the operation to the layer below immediately, and sets up a timer to repeat the operation.
     * To end the operation timer (when a valid response is received), use `terminateOperationTimeout`
     * @param targetNodeId The ID of the node that shall receive the operation
     * @param operation The operation to send immediately and repeat on a timer. If operation is null, nothing happens
     */
    private void executeOperationOnTimeout(short targetNodeId, RoutingInformationProtocolOperation operation) {
        if (operation == null)
            return;

        try {
            latestDataAccess.acquire();
            latestOperation = operation;
            operationResponseTimeout = new Timer();
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
                latestOperation instanceof RoutingInformationProtocolRequest
                && receivedOperation instanceof RoutingInformationProtocolResponse responseOperation
                && latestNodeId == responseOperation.getNodeId()
        ) {
            terminateOperationTimeout();
            ripServiceUserInterface.distanceTableIndication(responseOperation.getNodeId(), responseOperation.getDistanceTable());
        } else if (
                latestOperation instanceof RoutingInformationProtocolSet setOperation
                && receivedOperation instanceof RoutingInformationProtocolNotification notificationOperation
                && setOperation.getNodeAId() == notificationOperation.getNodeAId()
                && setOperation.getNodeBId() == notificationOperation.getNodeBId()
        ) {
            // `getInvertedOrNull` can return null if the operation has already been inverted once.
            RoutingInformationProtocolSet invertedSet = setOperation.getInvertedOrNull();
            short nodeBId = setOperation.getNodeBId();
            terminateOperationTimeout();

            if (invertedSet != null) {
                executeOperationOnTimeout(nodeBId, invertedSet);
            } else {
                // NodeA and NodeB have already been inverted.
                // As such, the value of the original NodeA is now on NodeB, and vice versa.
                // For this reason, we do (B, A, Cost) instead of the usual (A, B, Cost)
                ripServiceUserInterface.linkCostIndication(
                        notificationOperation.getNodeBId(),
                        notificationOperation.getNodeAId(),
                        notificationOperation.getCost()
                );
            }
        }
    }

    private boolean isNodeValid(short nodeId) {
        return nodeId <= networkTopology.getNodeCount() && nodeId >= 1;
    }

    private boolean isLinkValid(short firstNodeId, short secondNodeId) {
        return networkTopology.getCost(firstNodeId, secondNodeId) != null;
    }

    private boolean isCostValid(int linkCost) {
        return (linkCost > 0 && linkCost < 16) || linkCost == -1;
    }
}
