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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RoutingInformationProtocol implements UnicastServiceUserInterface {
    private final short MANAGER_ID = 0;
    private final int PORT = 520;
    private short nodeID;
    private Short[] linkCosts;
    private short[] distanceVector;
    private Short[][] nodesDistance;
    private UnicastServiceInterface unicastInterface;


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

        this.linkCosts = new Short[networkTopology.getNodeCount()];

        for (int i = 0; i < linkCosts.length; i++) {
            if(i == this.nodeID - 1){
                this.linkCosts[i] = 0;
            }
            else{
                this.linkCosts[i] = networkTopology.getCost((short)(this.nodeID - 1), (short)(i + 1));
            }
        }

        distanceVector = new short[networkTopology.getNodeCount()];
        updateDistanceVector();

        nodesDistance = new Short[networkTopology.getNodeCount()][networkTopology.getNodeCount()];

        for (int i = 0; i < nodesDistance.length; i++) {
            if(linkCosts[i] == null || i == this.nodeID - 1){
                continue;
            }

            Arrays.fill(nodesDistance[i], (short) -1);
        }

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::notifyNeighbors, timeoutSeconds, timeoutSeconds, TimeUnit.SECONDS);
    }

    private void updateDistanceVector() {
        short[] oldDistanceVector = distanceVector.clone();
        calculateDistanceVector();
        if(!Arrays.equals(oldDistanceVector, this.distanceVector)){
            notifyNeighbors();
        }
    }

    private void calculateDistanceVector(){
        for (int i = 0; i < this.distanceVector.length; i++) {
            if (i == this.nodeID - 1){
                this.distanceVector[i] = 0;
                continue;
            }

            short minDistance = -1;
            for (int j = 0; j < this.linkCosts.length; j++){
                if (this.linkCosts[j] == null || this.linkCosts[j] == -1) {
                    continue;
                }

                short newDistance = (short) (this.linkCosts[j] + this.nodesDistance[j][i]);
                if ((minDistance == -1 || newDistance < minDistance) && newDistance != -1){
                    minDistance = newDistance;
                }
            }
            this.distanceVector[i] = minDistance;
        }
    }

    private void notifyNeighbors(){
        for (int i = 0; i < this.linkCosts.length; i++) {
            int id = i + 1;
            if (id != this.nodeID && this.linkCosts[i] != null && this.linkCosts[i] != -1){

                short neighborID = (short) id;
                RoutingInformationProtocolIndication ripInd = new RoutingInformationProtocolIndication(neighborID, distanceVector);
                unicastInterface.UPDataReq(neighborID, ripInd.toString());
            }
        }
    }

    private void sendCost(short targetNeighborId){
        short linkCost = this.linkCosts[targetNeighborId - 1] != null ? this.linkCosts[targetNeighborId - 1] : -1;

        RoutingInformationProtocolNotification ripNtf = new RoutingInformationProtocolNotification(this.nodeID, targetNeighborId, linkCost);
        unicastInterface.UPDataReq(MANAGER_ID, ripNtf.toString());
    }

    private void setLinkCost(short neighborId, short cost){
        linkCosts[neighborId - 1] =  cost;

        if(cost == -1){
            Arrays.fill(nodesDistance[neighborId - 1], (short)-1);
        }

        updateDistanceVector();
        sendCost(neighborId);
    }

    private void updateNeighborsDistanceVectors(short neighborID, short[] distanceVector){
        for (int i=0; i < nodesDistance[neighborID -1].length; i++){
            nodesDistance[neighborID -1][i] = distanceVector[i];
        }
        updateDistanceVector();
    }

    private void sendDistanceTable(){
        int neighborsCount = 0;
        for (Short linkCost : linkCosts) {
            if (linkCost != null) {
                neighborsCount++;
            }
        }

        int[] neighborsIndexes =  new int[neighborsCount];
        for (int i = 0; i < linkCosts.length; i++) {
            if (linkCosts[i] != null) {
                neighborsIndexes[i] = i;
            }
        }

        short[][] distanceTable = new short[neighborsCount + 1][nodesDistance.length];

        distanceTable[0] = distanceVector.clone();

        for (int i = 1; i < distanceTable.length; i++) {
            for (int j = 0; i < distanceTable[i].length; i++) {
                if(linkCosts[neighborsIndexes[i - 1]] == -1 || nodesDistance[neighborsIndexes[i - 1]][j] == -1){
                    distanceTable[i][j] = -1;
                } else {
                    distanceTable[i][j] = (short) (linkCosts[neighborsIndexes[i - 1]] + nodesDistance[neighborsIndexes[i - 1]][j]);
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
