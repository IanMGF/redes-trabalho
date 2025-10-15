import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UnicastProtocol implements UnicastServiceInterface, Runnable {
    private static final String INITIAL_STRING = "UPDREQPDU";
    private static final Pattern PATTERN = Pattern.compile(INITIAL_STRING + " ([0-9]+) ((.|\n|\r)*)");

    private short ucsap_id;
    private short port;

    private DatagramSocket socket;
    private final UnicastServiceUserInterface user_interface;

    private final UnicastConfiguration configuration;

    UnicastProtocol(short ucsap_id, short port, UnicastServiceUserInterface user_interface) {
        this.ucsap_id = ucsap_id;
        this.port = port;
        this.user_interface = user_interface;

        this.configuration = UnicastConfiguration.LoadFromFile(new File("unicast.conf"));
        IPAddressAndPort address_and_port = configuration.GetAddress(ucsap_id);
        if (address_and_port.port != port) {
            throw new IllegalArgumentException("Self not found in configuration file");
        }

        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

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

    private String UnpackData(String protocol_data_unit) {
        Matcher matcher = PATTERN.matcher(protocol_data_unit);
        if (!matcher.matches()) {
            throw new RuntimeException("Error trying to unpack Unicast Data Unit:\n\"%s\"".formatted(protocol_data_unit));
        }
        String size_str = matcher.group(1);
        String unpacked_data = matcher.group(2);
        byte[] unpacked_bytes = unpacked_data.getBytes(StandardCharsets.UTF_8);
        int size = Integer.parseInt(size_str);

        if (unpacked_bytes.length > size) {
            unpacked_bytes = Arrays.copyOfRange(unpacked_bytes, 0, size);
        } else if (unpacked_bytes.length < size) {
            // Pad the result
            unpacked_bytes = Arrays.copyOf(unpacked_bytes, size);
        }

        unpacked_data = new String(unpacked_bytes, StandardCharsets.UTF_8);

        return unpacked_data;
    }

    @Override
    public boolean UPDataReq(short id, String data) {
        InetAddress address;
        String packed_data = PackData(data);
        int size = packed_data.getBytes(StandardCharsets.UTF_8).length;

        IPAddressAndPort address_and_port = configuration.GetAddress(id);

        if (address_and_port == null) {
            return false;
        }

        byte[] data_bytes = packed_data.getBytes();
        DatagramPacket packet = new DatagramPacket(data_bytes, size, address_and_port.address, address_and_port.port);
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

        while (true) {
            if(Thread.currentThread().isInterrupted()) {
                break;
            }

            byte[] recv_buffer = new byte[1024];
            packet = new DatagramPacket(recv_buffer, 1024);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            byte[] data = packet.getData();
            String data_str = new String(data, StandardCharsets.UTF_8);
            String unpacked;
            unpacked = UnpackData(data_str);

            InetAddress sender_address = packet.getAddress();
            short sender_port = (short) packet.getPort();

            short ucsap_id = configuration.GetId(new IPAddressAndPort(sender_address, sender_port));

            user_interface.UPDataInd(ucsap_id, unpacked);
        }
    }
}
