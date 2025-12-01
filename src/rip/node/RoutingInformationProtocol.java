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
import java.util.*;
import java.util.concurrent.Semaphore;


public class RoutingInformationProtocol implements UnicastServiceUserInterface {
    private final short MANAGER_ID = 0;
    private short nodeID;
    private Integer[] linkCosts;
    private int[] distanceVector;
    private Integer[][] nodeDistances;
    private UnicastServiceInterface unicastInterface;

    private final Semaphore distanceTableAccess;
    private final Semaphore linkCostsAccess;
    private final Semaphore nodeDistancesAccess;


    public RoutingInformationProtocol(short nodeID, int port) {
        this(nodeID, 10, port);
    }

    public RoutingInformationProtocol(short nodeID, long timeoutSeconds, int port) {
        distanceTableAccess = new Semaphore(1);
        nodeDistancesAccess = new Semaphore(1);
        linkCostsAccess = new Semaphore(1);
        File ripConfigurationFile = new File("rip.conf");
        RoutingInformationConfiguration networkTopology = null;
        try {
            networkTopology = RoutingInformationConfiguration.loadFromFile(ripConfigurationFile);
        } catch (FileNotFoundException fnfe) {
            System.err.println("Erro : Não foi encontrado arquivo de configuração do Routing Information Protocol ('rip.conf')");
        } catch (InvalidFormatException ife) {
            System.err.printf("Erro: Linha do arquivo de configuração Routing Information Protocol não seguem o formato <RIPNode_1> <RIPNode_2> <custo>: '%s'\n", ife.getText());
        } catch (NonIncrementalIdsException niie) {
            System.err.println("Erro: Ids dos nós no arquivo de configuração do Routing Information Protocol('rip.conf') não estão seguindo uma sequência incremental");
        } catch (InvalidNodeIdException inie) {
            System.err.printf("Erro: Id[%d] do(s) nó(s) no arquivo de configuração do Routing Information Protocol('rip.conf') não é um número inteiro entre 1 e 15\n", inie.getNodeId());
        } catch (InvalidCostException ice) {
            System.err.printf("Erro: Custo %d presente no arquivo de configuração do Routing Information Protocol ('rip.conf') não é um número inteiro entre 1 e 15\n", ice.getCost());
        } catch (RepeatedLinkException rle) {
            System.err.printf("Erro: Par não-ordenado de IDs ([%d], [%d]) encontrado mais de uma vez no arquivo de configuração do Routing Information Protocol('rip.conf')\n", rle.getNodeAId(), rle.getNodeBId());
        }

        if (networkTopology == null) {
            return;
        }

        this.nodeID = nodeID;
        if (nodeID > networkTopology.getNodeCount() || nodeID < 1) {
            System.err.printf("Erro: Id próprio (%d) não encontrado no arquivo de configuração do Routing Information Protocol('rip.conf')\n", this.nodeID);
            return;
        }


        this.unicastInterface = null;

        // Default port of the Routing Information Protocol
        try {
            unicastInterface = new UnicastProtocol(nodeID, port, this);
        } catch (IllegalArgumentException iae) {
            System.err.println("Erro : Conjunto ID e Porta não foram encontrados no arquivo de configuração do Unicast");
        } catch (FileNotFoundException fnfe) {
            System.err.println("Erro : Não foi encontrado arquivo de configuração do Unicast ('unicast.conf')");
        } catch (SocketException se) {
            System.err.printf("Erro : Não foi possível inicar atrelar socket à porta %s\n", port);
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
        nodeDistances = new Integer[networkTopology.getNodeCount()][networkTopology.getNodeCount()];
        updateDistanceVector();

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
    }

    /**
     * Updates {@link #distanceVector}, by calculating the new distance vector
     * (using {@link #calculateNewDistanceVector(int[])}). If the resulting vector is different from the current vector,
     * calls {@link #notifyNeighbors()} to notify neighbor nodes about the update.
     */
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
            // Can only happen if the thread is interrupted while waiting for the semaphore, which should never happen
            throw new RuntimeException(e);
        }
    }


    /**
     * Executes the calculation of the new distance vector, using {@link #linkCosts}.
     * This operation locks {@link #linkCostsAccess} until it is finished.
     * @param currDistanceVector The currently existing distance vector.
     * @return The new distance vector, based on {@link #linkCosts}
     */
    private int[] calculateNewDistanceVector(int[] currDistanceVector){
        int[] newDistanceVector = currDistanceVector.clone();
        for (int i = 0; i < currDistanceVector.length; i++) {
            int equivalentId = i + 1;
            if (equivalentId == this.nodeID){
                newDistanceVector[i] = 0;
                continue;
            }

            short minDistance = getMinDistance(i);
            newDistanceVector[i] = minDistance;
        }
        return newDistanceVector;
    }

    /**
     * Helper function to calculate the minimum distance to a specific ID
     */
    private short getMinDistance(int nodeId) {
        short minDistance = -1;
        try {
            nodeDistancesAccess.acquire();
            for (int j = 0; j < this.linkCosts.length; j++) {
                if (this.linkCosts[j] == null || this.linkCosts[j] == -1 || this.nodeDistances[j][nodeId] == null) {
                    continue;
                }

                short newDistance = (short) (this.linkCosts[j] + this.nodeDistances[j][nodeId]);
                if ((minDistance == -1 || newDistance < minDistance) && newDistance != -1) {
                    minDistance = newDistance;
                }
            }
            nodeDistancesAccess.release();
            return minDistance;
        } catch(InterruptedException e) {
            // Can only happen if the thread is interrupted while waiting for the semaphore, which should never happen
            throw new RuntimeException(e);
        }
    }

    /**
     * Notifies the neighbor nodes about a cost update
     */
    private void notifyNeighbors(){
        try {
            linkCostsAccess.acquire();
            for (int i = 0; i < this.linkCosts.length; i++) {
                short equivalentId = (short) (i + 1);
                if (equivalentId != this.nodeID && this.linkCosts[i] != null && this.linkCosts[i] != -1) {

                    distanceTableAccess.acquire();
                    RoutingInformationProtocolIndication ripInd = new RoutingInformationProtocolIndication(equivalentId, distanceVector);
                    distanceTableAccess.release();
                    sendOperation(equivalentId, ripInd);
                }
            }
            linkCostsAccess.release();
        } catch (InterruptedException e) {
            // Can only happen if the thread is interrupted while waiting for the semaphore, which should never happen
            throw new RuntimeException(e);
        }
    }

    /**
     * Sends the cost of it's link with another node to the manager entity
     * @param targetNeighborId The ID of the node that, with this entity, makes the link
     */
    private void sendCost(short targetNeighborId){
        int targetNeighborIndex = targetNeighborId - 1;
        try {
            linkCostsAccess.acquire();
            int linkCost = this.linkCosts[targetNeighborIndex] != null ? this.linkCosts[targetNeighborIndex] : -1;
            linkCostsAccess.release();
            RoutingInformationProtocolNotification ripNtf = new RoutingInformationProtocolNotification(this.nodeID, targetNeighborId, linkCost);
            sendOperation(MANAGER_ID, ripNtf);
        } catch (InterruptedException e) {
            // Can only happen if the thread is interrupted while waiting for the semaphore, which should never happen
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets the new link cost between this node and the node defined by neighborId
     * @param neighborId ID of the neighbor node that, with this entity, makes the link
     * @param cost The new cost of the link
     */
    private void setLinkCost(short neighborId, int cost){
        try {
            int neighborIndex = neighborId - 1;
            linkCostsAccess.acquire();
            linkCosts[neighborIndex] = cost;
            linkCostsAccess.release();

            if (cost == -1) {
                nodeDistancesAccess.acquire();
                Arrays.fill(nodeDistances[neighborIndex], -1);
                nodeDistancesAccess.release();
            }

            updateDistanceVector();
            sendCost(neighborId);
        } catch (InterruptedException e) {
            // Can only happen if the thread is interrupted while waiting for the semaphore, which should never happen
            throw new RuntimeException(e);
        }
    }

    /**
     * Updates the distance vector as a reaction of receiving a new distance vector from a neighbor node
     * @param neighborID The ID of the neighbor that sent the new distance vector
     * @param distanceVector The current distance vector
     */
    private void updateNeighborsDistanceVectors(short neighborID, int[] distanceVector){
        try {
            int neighborIndex = neighborID - 1;
            nodeDistancesAccess.acquire();
            for (int i = 0; i < nodeDistances[neighborIndex].length; i++) {
                nodeDistances[neighborIndex][i] = distanceVector[i];
            }
            nodeDistancesAccess.release();
            updateDistanceVector();
        } catch (InterruptedException e) {
            // Can only happen if the thread is interrupted while waiting for the semaphore, which should never happen
            throw new RuntimeException(e);
        }
    }

    /**
     * Sends it's distance table to Manager as a response.
     * Note: Most of the computation in this function is dedicated to map relative and global indexes of neighbors
     * Ex: If Node 4 has neighbors 2, 6, 8 and 10, this function will create a vector mapping them as:
     * 0 -> 2
     * 1 -> 6
     * 2 -> 8
     * 3 -> 10
     */
    private void sendDistanceTable(){
        try {
            linkCostsAccess.acquire();

            // Calculates how many neighbors the node has
            int neighborsCount = 0;
            for (Integer linkCost : linkCosts) {
                if (linkCost != null) {
                    neighborsCount++;
                }
            }

            // Traverses all nodes in order, adding only neighbors to the vector
            List<Integer> neighborIndexes = new ArrayList<>(neighborsCount);
            for (int i = 0; i < linkCosts.length; i++) {
                if (linkCosts[i] != null) {
                    neighborIndexes.add(i);
                }
            }

            // Builds the distance table
            int[][] distanceTable = new int[neighborsCount + 1][nodeDistances.length];

            distanceTableAccess.acquire();
            distanceTable[0] = distanceVector.clone();
            distanceTableAccess.release();

            // Sets up values in distance table
            for (int i = 1; i < distanceTable.length; i++) {
                int lineIdx = i - 1;
                for (int j = 0; i < distanceTable[i].length; i++) {
                    int neighborIdx = neighborIndexes.get(lineIdx);
                    if (linkCosts[neighborIdx] == -1 || nodeDistances[neighborIdx][j] == -1) {
                        distanceTable[i][j] = -1;
                    } else {
                        distanceTable[i][j] = (linkCosts[neighborIdx] + nodeDistances[neighborIdx][j]);
                    }
                }
            }

            linkCostsAccess.release();
            RoutingInformationProtocolResponse ripRsp = new RoutingInformationProtocolResponse(this.nodeID, distanceTable);
            sendOperation(MANAGER_ID, ripRsp);
        } catch (InterruptedException e) {
            // Can only happen if the thread is interrupted while waiting for the semaphore, which should never happen
            throw new RuntimeException(e);
        }
    }

    /**
     * Sends the operation, using UPDataReq from Unicast.
     * Will NOT send if the data unit of the operation is bigger than 512 bytes.
     * @param nodeId The ID of the target node
     * @param operation The operation to be sent
     */
    public void sendOperation(short nodeId, RoutingInformationProtocolOperation operation) {
        String dataUnit = operation.toString();
        if(dataUnit.getBytes().length <= 512) {
            unicastInterface.UPDataReq(nodeId, dataUnit);
        }
    }

    public void UPDataInd(short id, String data) {
        RoutingInformationProtocolOperation ripOperation = RoutingInformationProtocolOperationParser.parse(data);

        if (
                ripOperation instanceof RoutingInformationProtocolGet get
                && id == MANAGER_ID && get.getNodeAId() == this.nodeID
        ) {
            sendCost(get.getNodeBId());
        } else if (
                ripOperation instanceof RoutingInformationProtocolSet set
                && id == MANAGER_ID && set.getNodeAId() == this.nodeID
                && linkCosts[set.getNodeBId()] != null
        ) {
            setLinkCost(set.getNodeBId(),  set.getCost());
        } else if (
                ripOperation instanceof RoutingInformationProtocolIndication indication
                && indication.getNodeId() != this.nodeID
        ) {
            updateNeighborsDistanceVectors(indication.getNodeId(), indication.getDistanceVector());
        } else if (ripOperation instanceof RoutingInformationProtocolRequest && id == MANAGER_ID) {
            sendDistanceTable();
        }
    }
}
