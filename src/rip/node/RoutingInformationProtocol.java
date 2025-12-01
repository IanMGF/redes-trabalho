package rip.node;

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
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;


public class RoutingInformationProtocol implements UnicastServiceUserInterface {
    private final short MANAGER_ID = 0;
    private short nodeID;
    private Integer[] linkCosts;
    private int[] distanceVector;
    private Integer[][] nodeDistances;
    private UnicastServiceInterface unicastInterface;

    private Semaphore distanceTableAccess;


    public RoutingInformationProtocol(short nodeID) {
        this(nodeID,10);
    }

    public RoutingInformationProtocol(short nodeID, long timeoutSeconds) {
        File ripConfigurationFile = new File("rip.conf");
        RoutingInformationConfiguration networkTopology = null;
        try {
            networkTopology = RoutingInformationConfiguration.loadFromFile(ripConfigurationFile);
        } catch (FileNotFoundException fnfe) {
            System.err.println("Erro : Não foi encontrado arquivo de configuração do Routing Information Protocol ('rip.conf')");
        } catch (InvalidFormatException ife) {
            System.err.printf("Erro: Linha do arquivo de configuração Routing Information Protocol não seguem o formato <RIPNode_1> <RIPNode_2> <custo>: '%s'\n", ife.getText());
        } catch (NonIncrementalIdsException niie) {
            System.err.println("Erro: Ids dos nós no arquivo de configuração do Routing Information Protocol('rip.conf') não estão no fomato incremental");
        } catch (InvalidNodeIdException inie) {
            System.err.printf("Erro: Id[%d] do(s) nó(s) no arquivo de configuração do Routing Information Protocol('rip.conf') não é um número inteiro entre 1 e 15\n", inie.getNodeId());
        } catch (RepeatedLinkException rle) {
            System.err.printf("Erro: Par não-ordenado de IDs ([%d], [%d]) encontrado mais de uma vez no arquivo de configuração do Routing Information Protocol('rip.conf')\n", rle.getNodeAId(), rle.getNodeBId());
        }

        if (networkTopology == null) {
            return;
        }

        if (nodeID >= networkTopology.getNodeCount() || nodeID < 0) {
            System.err.printf("Erro: Id próprio (%d) não encontrado no arquivo de configuração do Routing Information Protocol('rip.conf')\n", this.nodeID);
            return;
        }

        this.nodeID = nodeID;

        this.unicastInterface = null;

        // Default port of the Routing Information Protocol
        int PORT = 520;
        try {
            unicastInterface = new UnicastProtocol(nodeID, PORT, this);
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

        if (unicastInterface == null) {
            return;
        }

        this.linkCosts = new Integer[networkTopology.getNodeCount()];

        for (int i = 0; i < linkCosts.length; i++) {
            if(i == this.nodeID - 1){
                this.linkCosts[i] = 0;
            }
            else{
                this.linkCosts[i] = networkTopology.getCost((short)(this.nodeID - 1), (short)(i + 1));
            }
        }

        distanceVector = new int[networkTopology.getNodeCount()];
        updateDistanceVector();

        nodeDistances = new Integer[networkTopology.getNodeCount()][networkTopology.getNodeCount()];

        for (int i = 0; i < nodeDistances.length; i++) {
            if(linkCosts[i] == null || i == this.nodeID - 1){
                continue;
            }

            Arrays.fill(nodeDistances[i], -1);
        }

        Timer periodicSender = new Timer();
        periodicSender.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                notifyNeighbors();
            }
        }, timeoutSeconds * 1_000, timeoutSeconds * 1_000);

        distanceTableAccess = new Semaphore(1);
    }

    private void updateDistanceVector() {
        try {
            distanceTableAccess.acquire();
            int [] newDistanceVector = calculateNewDistanceVector(distanceVector);
            if(!Arrays.equals(newDistanceVector, this.distanceVector)){
                this.distanceVector = newDistanceVector;
                distanceTableAccess.release();
                notifyNeighbors();
            } else {
                distanceTableAccess.release();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    private int[] calculateNewDistanceVector(int[] currDistanceVector){
        int[] newDistanceVector = currDistanceVector.clone();
        for (int i = 0; i < currDistanceVector.length; i++) {
            int equivalentId = i + 1;
            if (equivalentId == this.nodeID){
                newDistanceVector[i] = 0;
                continue;
            }

            short minDistance = -1;
            for (int j = 0; j < this.linkCosts.length; j++){
                if (this.linkCosts[j] == null || this.linkCosts[j] == -1) {
                    continue;
                }

                short newDistance = (short) (this.linkCosts[j] + this.nodeDistances[j][i]);
                if ((minDistance == -1 || newDistance < minDistance) && newDistance != -1){
                    minDistance = newDistance;
                }
            }
            newDistanceVector[i] = minDistance;
        }
        return newDistanceVector;
    }

    private void notifyNeighbors(){
        try {
            for (int i = 0; i < this.linkCosts.length; i++) {
                int equivalentId = i + 1;
                if (equivalentId != this.nodeID && this.linkCosts[i] != null && this.linkCosts[i] != -1) {

                    short neighborID = (short) equivalentId;
                    distanceTableAccess.acquire();
                    RoutingInformationProtocolIndication ripInd = new RoutingInformationProtocolIndication(neighborID, distanceVector);
                    distanceTableAccess.release();
                    unicastInterface.UPDataReq(neighborID, ripInd.toString());
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendCost(short targetNeighborId){
        int linkCost = this.linkCosts[targetNeighborId - 1] != null ? this.linkCosts[targetNeighborId - 1] : -1;

        RoutingInformationProtocolNotification ripNtf = new RoutingInformationProtocolNotification(this.nodeID, targetNeighborId, linkCost);
        unicastInterface.UPDataReq(MANAGER_ID, ripNtf.toString());
    }

    private void setLinkCost(short neighborId, int cost){
        linkCosts[neighborId - 1] =  cost;

        if(cost == -1){
            Arrays.fill(nodeDistances[neighborId - 1], -1);
        }

        updateDistanceVector();
        sendCost(neighborId);
    }

    private void updateNeighborsDistanceVectors(short neighborID, int[] distanceVector){
        for (int i = 0; i < nodeDistances[neighborID -1].length; i++){
            nodeDistances[neighborID -1][i] = distanceVector[i];
        }
        updateDistanceVector();
    }

    private void sendDistanceTable(){
        int neighborsCount = 0;
        for (Integer linkCost : linkCosts) {
            if (linkCost != null) {
                neighborsCount++;
            }
        }

        int[] neighborIndexes =  new int[neighborsCount];
        for (int i = 0; i < linkCosts.length; i++) {
            if (linkCosts[i] != null) {
                neighborIndexes[i] = i;
            }
        }

        int[][] distanceTable = new int[neighborsCount + 1][nodeDistances.length];

        try {
            distanceTableAccess.acquire();
            distanceTable[0] = distanceVector.clone();
            distanceTableAccess.release();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        for (int i = 1; i < distanceTable.length; i++) {
            for (int j = 0; i < distanceTable[i].length; i++) {
                if(linkCosts[neighborIndexes[i - 1]] == -1 || nodeDistances[neighborIndexes[i - 1]][j] == -1){
                    distanceTable[i][j] = -1;
                } else {
                    distanceTable[i][j] = (linkCosts[neighborIndexes[i - 1]] + nodeDistances[neighborIndexes[i - 1]][j]);
                }
            }
        }

        RoutingInformationProtocolResponse ripRsp = new RoutingInformationProtocolResponse(this.nodeID, distanceTable);
        unicastInterface.UPDataReq(MANAGER_ID, ripRsp.toString());
    }

    public void UPDataInd(short id, String data) {
        RoutingInformationProtocolOperation ripOperation = RoutingInformationProtocolOperationParser.parse(data);

        if (ripOperation instanceof RoutingInformationProtocolGet get && id == MANAGER_ID && get.getNodeAId() == this.nodeID) {
            sendCost(get.getNodeBId());
        } else if (ripOperation instanceof RoutingInformationProtocolSet set && id == MANAGER_ID &&
                set.getNodeAId() == this.nodeID &&  linkCosts[set.getNodeBId()] != null) {
            setLinkCost(set.getNodeBId(),  set.getCost());
        } else if (ripOperation instanceof RoutingInformationProtocolIndication indication && indication.getNodeId() != this.nodeID) {
            updateNeighborsDistanceVectors(indication.getNodeId(), indication.getDistanceVector());
        } else if (ripOperation instanceof RoutingInformationProtocolRequest && id == MANAGER_ID) {
            sendDistanceTable();
        }
    }
}
