import exceptions.InvalidFormatException;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author Ian Marcos Gomes e Freitas
 * @author João Roberto de Moraes Neto
 */
public class UnicastProtocol implements UnicastServiceInterface {
    private static final String INITIAL_STRING = "UPDREQPDU";
    private static final Pattern PATTERN = Pattern.compile(INITIAL_STRING + " ([0-9]+) ((.|\n|\r)*)");

    private final short ucsapId;

    private final DatagramSocket socket;
    private final Thread listenerThread;
    private final UnicastListener listener;

    private final UnicastConfiguration configuration;

    /**
     * @param ucsapId Unicast service ID, defined in the `unicast.conf` file
     * @param port Port at which the protocol will start
     * @param userInterface Service user interface. It's `UPDataInd` method will be called whenever a new message is received
     * @throws IllegalArgumentException Exception thrown if the port passed to the initializer does not match the one found in `unicast.conf`
     * @throws IOException Exception thrown if the file failed to close
     */
    UnicastProtocol(short ucsapId, short port, UnicastServiceUserInterface userInterface) throws IllegalArgumentException, IOException, InvalidFormatException {
        this.ucsapId = ucsapId;

        this.configuration = UnicastConfiguration.LoadFromFile(new File("unicast.conf"));
        IPAddressAndPort ipAddressAndPort = configuration.GetAddress(ucsapId);
        if (ipAddressAndPort.port != port) {
            throw new IllegalArgumentException("Self not found in configuration file");
        }

        socket = new DatagramSocket(port);

        listener = new UnicastListener(socket, userInterface, configuration);
        listenerThread = new Thread(listener);
    }

    /**
     * Packs a string into the Unicast Protocol Data Unit format.
     *
     * @param data The data to be wrapped into a protocol data unit
     * @return The string of bytes containing the data, wrapped in a protocol data unit
     * @throws RuntimeException If the data sent has over 1009 bytes, which would result in a PDU longer than 1024 bytes
     */
    static String PackData(String data) {
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
    static String UnpackData(String protocolDataUnit) throws InvalidFormatException {
        Matcher matcher = PATTERN.matcher(protocolDataUnit);
        if (!matcher.matches()) {
            throw new InvalidFormatException("Error trying to unpack Unicast Data Unit:\n\"%s\"".formatted(protocolDataUnit));
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

    public void stop() {
        listenerThread.interrupt();
        listener.stop();
    }

    public short getId() {
        return ucsapId;
    }
}

class UnicastListener implements Runnable {
    private final DatagramSocket socket;
    private final UnicastServiceUserInterface userInterface;
    private final UnicastConfiguration configuration;
    private boolean running;

    UnicastListener(DatagramSocket socket, UnicastServiceUserInterface userInterface, UnicastConfiguration configuration) {
        this.socket = socket;
        this.userInterface = userInterface;
        this.configuration = configuration;
        running = true;
    }

    @Override
    public void run() {
        DatagramPacket packet;

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
            try {
                unpacked = UnicastProtocol.UnpackData(dataStr);
            } catch (InvalidFormatException e) {
                throw new RuntimeException(e);
            }

            InetAddress senderAddress = packet.getAddress();
            short senderPort = (short) packet.getPort();

            short ucsapId = configuration.GetId(new IPAddressAndPort(senderAddress, senderPort));

            userInterface.UPDataInd(ucsapId, unpacked);
        }
    }

    public void stop() {
        socket.close();
        running = false;
    }
}