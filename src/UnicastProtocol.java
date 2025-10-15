import java.io.File;
import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UnicastProtocol implements UnicastServiceInterface {
    private static final String INITIAL_STRING = "UPDREQPDU";
    private static final Pattern PATTERN = Pattern.compile(INITIAL_STRING + " ([0-9]+) ((.|\n|\r)*)");

    private short ucsap_id;
    private short port;

    private final UnicastConfiguration configuration;

    UnicastProtocol(short ucsap_id, short port) {
        this.ucsap_id = ucsap_id;
        this.port = port;

        this.configuration = UnicastConfiguration.LoadFromFile(new File("unicast.conf"));
        IPAddressAndPort address_and_port = configuration.GetAddress(ucsap_id);
        if (address_and_port.port != port) {
            throw new IllegalArgumentException("Self not found in configuration file");
        }
    }

    private String PackData(String data) {
        int size = data.length();

        return INITIAL_STRING +
                " " +
                size +
                " " +
                data;
    }

    private String UnpackData(String protocol_data_unit) throws ParseException {
        Matcher matcher = PATTERN.matcher(protocol_data_unit);
        String size_str = matcher.group(1);
        String unpacked_data = matcher.group(2);

        int size = Integer.parseInt(size_str);

        if (unpacked_data.length() > size) {
            unpacked_data = unpacked_data.substring(0, size);
        } else if (unpacked_data.length() < size) {
            // Pad the result
            unpacked_data = String.format("%" + size + "s", unpacked_data);
        }

        return unpacked_data;
    }

    @Override
    public boolean UPDataReq(short id, String data) {
        String packed_data = PackData(data);
        // Send through UDP
        return false;
    }
}
