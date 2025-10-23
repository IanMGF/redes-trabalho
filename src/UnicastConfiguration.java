import exceptions.InvalidFormatException;
import exceptions.InvalidPortException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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

    private static final Pattern PATTERN = Pattern.compile(
        "([0-9]+) ([a-zA-Z0-9.]+) ([0-9]+)"
    );
    private final HashMap<Short, IPAddressAndPort> configurationMap =
        new HashMap<>();

    /** Reads
     *
     * @param file The file to read the configuration from
     * @return A configuration loaded from the file
     * @throws FileNotFoundException
     * @throws UnknownHostException
     * @throws InvalidPortException
     * @throws InvalidFormatException
     */
    public static UnicastConfiguration LoadFromFile(File file)
        throws FileNotFoundException, UnknownHostException, InvalidPortException, InvalidFormatException {
        UnicastConfiguration configuration = new UnicastConfiguration();

        BufferedReader reader;
        reader = new BufferedReader(new FileReader(file));

        for (String line : reader.lines().toList()) {
            Matcher matcher = PATTERN.matcher(line);

            if (!matcher.matches()) {
                try {
                    reader.close();
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }
                throw new InvalidFormatException(
                    "Line does not follow format of <id> <host> <port>:\n\"%s\"".formatted(
                        line
                    )
                );
            }

            short ucsapId = (short) Integer.parseInt(matcher.group(1));
            String address_str = matcher.group(2);
            short port = (short) Integer.parseInt(matcher.group(3));

            if (port <= 1024) {
                try {
                    reader.close();
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }
                throw new InvalidPortException(
                    port, "Port less than 1024 found in file configuration"
                );
            }

            InetAddress address = null;
            address = InetAddress.getByName(address_str);

            IPAddressAndPort address_and_port = new IPAddressAndPort(
                address,
                port
            );

            configuration.configurationMap.put(ucsapId, address_and_port);
        }

        try {
            reader.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

        return configuration;
    }

    public IPAddressAndPort GetAddress(short ucsapId) {
        return configurationMap.get(ucsapId);
    }

    public short GetId(IPAddressAndPort address_and_port) {
        return configurationMap
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue().equals(address_and_port))
            .findFirst()
            .orElseThrow()
            .getKey();
    }
}
