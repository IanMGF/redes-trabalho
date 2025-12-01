package rip;

import exceptions.InvalidFormatException;
import exceptions.InvalidNodeIdException;
import exceptions.NonIncrementalIdsException;
import exceptions.RepeatedLinkException;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class RoutingInformationConfiguration {
    /** Private helper class to semantically encapsule the idea of a node link
     *
     * This class overrides the hashCode method, so two different NodeLink objects
     * can have the same hash, if they connect the same two nodes (independent of order)
     */
    private static class NodeLink {
        short nodeAId;
        short nodeBId;

        public NodeLink(short nodeAId, short nodeBId) {
            this.nodeAId = nodeAId;
            this.nodeBId = nodeBId;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof NodeLink nodeLink)) {
                return false;
            }

            return (nodeAId ==  nodeLink.nodeAId && nodeBId == nodeLink.nodeBId) || (nodeAId == nodeLink.nodeBId && nodeBId == nodeLink.nodeAId);
        }

        @Override
        public int hashCode() {
            HashSet<Short> set = new HashSet<>();
            set.add(nodeAId);
            set.add(nodeBId);
            return set.hashCode();
        }
    }
    private static final Pattern PATTERN = Pattern.compile("([0-9]+) ([0-9]+) ([0-9]+)");
    private final HashMap<NodeLink, Integer> configuration = new HashMap<>();
    private int nodeCount = 0;

    /** Reads a configuration file, returning a RoutingInformationConfiguration object built from such file
     *
     * @param file The file to read the configuration from
     * @return A configuration loaded from the file
     * @throws FileNotFoundException If `file` could not be found
     * @throws InvalidNodeIdException If a node ID is not in range [1, 15]
     * @throws InvalidFormatException If the format of the file is wrong
     * @throws NonIncrementalIdsException If there is a gap in the IDs registered (example: 1, 2, 4, 5)
     */
    public static RoutingInformationConfiguration loadFromFile(File file)
            throws FileNotFoundException, InvalidNodeIdException, InvalidFormatException, NonIncrementalIdsException, RepeatedLinkException {
        RoutingInformationConfiguration configuration = new RoutingInformationConfiguration();

        BufferedReader reader;
        reader = new BufferedReader(new FileReader(file));
        final HashSet<Short> nodeIds = new HashSet<>();


        for (String line : reader.lines().toList()) {
            Matcher matcher = PATTERN.matcher(line);

            if (!matcher.matches()) {
                try {                    reader.close();
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }
                throw new InvalidFormatException(
                        "Line does not follow format of <node_id> <node_id> <cost>:\n\"%s\"".formatted(
                                line
                        ), line
                );
            }

            short nodeAId = (short) Integer.parseInt(matcher.group(1));
            short nodeBId = (short) Integer.parseInt(matcher.group(2));
            int cost = Integer.parseInt(matcher.group(3));

            if (nodeAId <= 0 || nodeAId >= 16
                || nodeBId <= 0 || nodeBId >= 16) {
                try {
                    reader.close();
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }
                throw new InvalidNodeIdException(
                        nodeAId, "Node ID not in range [1, 15]"
                );
            }

            if (cost <= 0 || cost >= 16) {
                try {
                    reader.close();
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }
                throw new InvalidNodeIdException(
                        nodeAId, "Node ID not in range [1, 15]"
                );
            }

            NodeLink nodeLink = new NodeLink(nodeAId, nodeBId);

            if (configuration.configuration.containsKey(nodeLink)) {
                throw new RepeatedLinkException(nodeLink.nodeAId, nodeLink.nodeBId, "Repeated link found in configuration file");
            } else {
                configuration.configuration.put(nodeLink, cost);
            }
            nodeIds.add(nodeLink.nodeAId);
            nodeIds.add(nodeLink.nodeBId);
        }

        try {
            reader.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

        short maxNodeId = nodeIds.stream().max(Short::compareTo).orElseThrow(RuntimeException::new);
        configuration.nodeCount = maxNodeId;
        for (short i=1; i<=maxNodeId; i++) {
            if (!nodeIds.contains(i)) {
                throw new NonIncrementalIdsException("Gap in IDs");
            }
        }
        return configuration;
    }

    public Integer getCost(short nodeAId, short nodeBId) {
        Integer cost = null;
        cost = configuration.get(new NodeLink(nodeAId, nodeBId));
        if (cost == null) {
            cost = configuration.get(new NodeLink(nodeBId, nodeAId));
        }
        return cost;
    }

    public int getNodeCount() {
        return nodeCount;
    }
}
