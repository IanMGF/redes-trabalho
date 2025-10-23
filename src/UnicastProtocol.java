import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// TODO: Move thread start and kill logic to UnicastProtocol class

/**
 * @author Ian Marcos Gomes e Freitas
 * @author João Roberto de Moraes Neto
 *
 *
 */
public class UnicastProtocol implements UnicastServiceInterface, Runnable {
    private static final String INITIAL_STRING = "UPDREQPDU";
    private static final Pattern PATTERN = Pattern.compile(INITIAL_STRING + " ([0-9]+) ((.|\n|\r)*)");

    private short ucsapId;
    private short port;

    private DatagramSocket socket;
    private final UnicastServiceUserInterface userInterface;

    private final UnicastConfiguration configuration;

    UnicastProtocol(short ucsapId, short port, UnicastServiceUserInterface userInterface) {
        this.ucsapId = ucsapId;
        this.port = port;
        this.userInterface = userInterface;

        this.configuration = UnicastConfiguration.LoadFromFile(new File("unicast.conf"));
        IPAddressAndPort ipAddressAndPort = configuration.GetAddress(ucsapId);
        if (ipAddressAndPort.port != port) {
            throw new IllegalArgumentException("Self not found in configuration file");
        }

        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Packs a string into the Unicast Protocol Data Unit format.
     *
     * @param data The data to be wrapped into a protocol data unit
     * @return The string of bytes containing the data, wrapped in a protocol data unit
     * @throws RuntimeException If the data sent has over 1009 bytes, which would result in a PDU longer than 1024 bytes
     */
    private String PackData(String data) {
        int size = data.getBytes().length;

        String pdu = INITIAL_STRING +
                " " +
                size +
                " " +
                data;

        if (pdu.getBytes(StandardCharsets.UTF_8).length > 1024) {
            throw new RuntimeException("Data too long");
        }

        return pdu;
    }


    /**
     * Unpacks a string in Protocol Data Unit format back into it's original data
     *
     * @param protocolDataUnit The string, in the format of the protocol data unit, to unpack
     * @return The original unpacked data
     * @throws RuntimeException If the string passed does NOT follow the format of the PDU
     */
    private String UnpackData(String protocolDataUnit) throws RuntimeException {
        Matcher matcher = PATTERN.matcher(protocolDataUnit);
        if (!matcher.matches()) {
            throw new RuntimeException("Error trying to unpack Unicast Data Unit:\n\"%s\"".formatted(protocolDataUnit));
        }
        String sizeStr = matcher.group(1);
        String unpackedData = matcher.group(2);
        byte[] unpackedBytes = unpackedData.getBytes(StandardCharsets.UTF_8);
        int size = Integer.parseInt(sizeStr);

        // Adjust size
        byte[] adjustedBytes = Arrays.copyOf(unpackedBytes, size);

        return new String(adjustedBytes, StandardCharsets.UTF_8);

    }

    @Override
    public boolean UPDataReq(short id, String data) {
        String packedData = PackData(data);
        int size = packedData.getBytes(StandardCharsets.UTF_8).length;

        IPAddressAndPort ipAddressAndPort = configuration.GetAddress(id);

        if (ipAddressAndPort == null) {
            return false;
        }

        byte[] dataBytes = packedData.getBytes();
        DatagramPacket packet = new DatagramPacket(dataBytes, size, ipAddressAndPort.address, ipAddressAndPort.port);
        try {
            socket.send(packet);
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    @Override
    public void run() {
        DatagramPacket packet;
        boolean running = true;

        while (running) {
            if(Thread.currentThread().isInterrupted()) {
                running = false;
            }

            byte[] receiveBuffer = new byte[1024];
            packet = new DatagramPacket(receiveBuffer, 1024);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            byte[] data = packet.getData();
            String dataStr = new String(data, StandardCharsets.UTF_8);
            String unpacked;
            unpacked = UnpackData(dataStr);

            InetAddress senderAddress = packet.getAddress();
            short senderPort = (short) packet.getPort();

            short ucsapId = configuration.GetId(new IPAddressAndPort(senderAddress, senderPort));

            userInterface.UPDataInd(ucsapId, unpacked);
        }
    }
}
