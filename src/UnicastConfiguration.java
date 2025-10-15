import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



public class UnicastConfiguration {
    private static final Pattern PATTERN = Pattern.compile("([0-9]+) ([a-zA-Z0-9.]+) ([0-9]+)");
    private final HashMap<Short, IPAddressAndPort> configuration_map = new HashMap<>();

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

            short ucsap_id = (short) Integer.parseInt(matcher.group(1));
            String address = matcher.group(2);
            short port = (short) Integer.parseInt(matcher.group(3));

            if (port <= 1024) {
                throw new RuntimeException("Port less than 1024 found in file configuration");
            }
            IPAddressAndPort address_and_port = new IPAddressAndPort(address, port);

            configuration.configuration_map.put(ucsap_id, address_and_port);
        }

        return configuration;
    }

    public IPAddressAndPort GetAddress(short ucsap_id) {
        return configuration_map.get(ucsap_id);
    }
    public short GetId(IPAddressAndPort address_and_port) {
        return configuration_map.entrySet().stream()
                .filter(entry -> {
                    IPAddressAndPort addr_and_port_on_set = entry.getValue();
                    InetAddress address_a, address_b;

                    try {
                        address_a = InetAddress.getByName(addr_and_port_on_set.address);
                    } catch (UnknownHostException e) {
                        return false;
                    }

                    try {
                        address_b = InetAddress.getByName(address_and_port.address);
                    } catch (UnknownHostException e) {
                        return false;
                    }

                    return (address_a.equals(address_b) && addr_and_port_on_set.port.equals(address_and_port.port));
                })
                .findFirst()
                .orElseThrow()
                .getKey();
    }
}
