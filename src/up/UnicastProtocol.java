package up;

import exceptions.DataSizeException;
import exceptions.InvalidFormatException;
import exceptions.InvalidPortException;

import java.io.File;
import java.io.FileNotFoundException;
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

    private static final String PDU_PREFIX = "UPDREQPDU";
    private static final Pattern PATTERN = Pattern.compile(PDU_PREFIX + " ([0-9]+) ((.|\n|\r)*)");

    // Despite the non-use of UCSApId, it is semantically incoherent to not store such information
    @SuppressWarnings("FieldCanBeLocal")
    private final short UCSApId;

    private final DatagramSocket socket;
    private final UnicastListener listener;

    private final UnicastConfiguration configuration;

    /**
     * @param UCSApId Unicast service ID, defined in the `unicast.conf` file
     * @param port Port at which the protocol will start
     * @param userInterface Service user interface. It's `UPDataInd` method will be called whenever a new message is received
     * @throws IllegalArgumentException If the port passed to the initializer does not match the one found in `unicast.conf`
     * @throws FileNotFoundException If `unicast.conf` was not found
     * @throws UnknownHostException If a host within `unicast.conf`could not be found
     * @throws InvalidFormatException If `unicast.conf` is not properly formatted
     * @throws InvalidPortException If there is an invalid port in the configuration file
     * @throws SocketException If the socket could not be opened or bound
     */
    public UnicastProtocol(
            short UCSApId,
            int port,
            UnicastServiceUserInterface userInterface
    )
        throws IllegalArgumentException, FileNotFoundException, UnknownHostException, SocketException, InvalidFormatException, InvalidPortException {
        this.UCSApId = UCSApId;

        this.configuration = UnicastConfiguration.LoadFromFile(
            new File("unicast.conf")
        );
        IPAddressAndPort ipAddressAndPort = configuration.GetAddress(UCSApId);
        if (ipAddressAndPort == null || ipAddressAndPort.port() != port) {
            throw new IllegalArgumentException(
                "Self not found in configuration file"
            );
        }

        socket = new DatagramSocket(port);

        listener = new UnicastListener(socket, userInterface, configuration);
        Thread listenerThread = new Thread(listener);

        listenerThread.start();
    }

    /**
     * Packs a string into the Unicast Protocol Data Unit format.
     *
     * @param data The data to be wrapped into a protocol data unit
     * @return The string of bytes containing the data, wrapped in a protocol data unit
     * @throws DataSizeException If the data sent has over 1009 bytes, which would result in a PDU longer than 1024 bytes
     */
    static String PackData(String data) throws DataSizeException {
        int size = data.getBytes().length;

        String pdu = PDU_PREFIX + " " + size + " " + data;

        if (pdu.getBytes(StandardCharsets.UTF_8).length > 1024) {
            throw new DataSizeException("Data too long");
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
    static String UnpackData(String protocolDataUnit)
        throws InvalidFormatException {
        Matcher matcher = PATTERN.matcher(protocolDataUnit);
        if (!matcher.matches()) {
            throw new InvalidFormatException(
                "Error trying to unpack Unicast Data Unit:\n\"%s\"".formatted(
                    protocolDataUnit
                ), protocolDataUnit
            );
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
        String packedData;
        try {
            packedData = PackData(data);
        } catch (DataSizeException dse) {
            return false;
        }
        int size = packedData.getBytes(StandardCharsets.UTF_8).length;

        IPAddressAndPort ipAddressAndPort = configuration.GetAddress(id);

        if (ipAddressAndPort == null) {
            return false;
        }

        byte[] dataBytes = packedData.getBytes();
        DatagramPacket packet = new DatagramPacket(
            dataBytes,
            size,
                ipAddressAndPort.address(),
                ipAddressAndPort.port()
        );
        try {
            socket.send(packet);
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    public void stop() {
        listener.stop();
    }
}