import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author Ian Marcos Gomes e Freitas
 * @author João Roberto de Moraes Neto
 *
 *
 */
public class UnicastConfiguration {
    private static final Pattern PATTERN = Pattern.compile("([0-9]+) ([a-zA-Z0-9.]+) ([0-9]+)");
    private final HashMap<Short, IPAddressAndPort> configurationMap = new HashMap<>();

    public static UnicastConfiguration LoadFromFile(File file) {
        UnicastConfiguration configuration = new UnicastConfiguration();

        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        for (String line: reader.lines().toList()) {
            Matcher matcher = PATTERN.matcher(line);

            if(!matcher.matches()) {
                throw new RuntimeException("Line does not follow format of <id> <host> <port>:\n\"%s\"".formatted(line));
            }

            short ucsapId = (short) Integer.parseInt(matcher.group(1));
            String address_str = matcher.group(2);
            short port = (short) Integer.parseInt(matcher.group(3));

            if (port <= 1024) {
                throw new RuntimeException("Port less than 1024 found in file configuration");
            }

            InetAddress address = null;
            try {
                address = InetAddress.getByName(address_str);
            } catch (UnknownHostException e) {
                throw new RuntimeException("Address not found: %s".formatted(address_str));
            }
            IPAddressAndPort address_and_port = new IPAddressAndPort(address, port);

            configuration.configurationMap.put(ucsapId, address_and_port);
        }

        return configuration;
    }

    public IPAddressAndPort GetAddress(short ucsapId) {
        return configurationMap.get(ucsapId);
    }
    public short GetId(IPAddressAndPort address_and_port) {
        return configurationMap.entrySet().stream()
                .filter(entry -> entry.getValue().equals(address_and_port))
                .findFirst()
                .orElseThrow()
                .getKey();
    }
}
