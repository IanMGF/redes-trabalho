import java.net.InetAddress;

/**
 * @author Ian Marcos Gomes e Freitas
 * @author João Roberto de Moraes Neto
 *
 *
 */
public class IPAddressAndPort {

    public InetAddress address;
    public Integer port;

    IPAddressAndPort(InetAddress address, int port) {
        this.address = address;
        this.port = port;
    }

    /**
     * Checks equality between two instances of the IPAddressAndPort class
     *
     * @param other The instance which is going to be compared to the instance that is executing the method.
     * @return The boolean that represents the result of the equality check
     */
    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof IPAddressAndPort other_addr)) {
            return false;
        }

        return (
            address.equals(other_addr.address) && port.equals(other_addr.port)
        );
    }
}
