package up;

import java.net.InetAddress;

/**
 * @author Ian Marcos Gomes e Freitas
 * @author João Roberto de Moraes Neto
 *
 *
 */
public record IPAddressAndPort(InetAddress address, Integer port) {
    /**
     * Checks equality between two instances of the unicast.IPAddressAndPort class
     *
     * @param other The instance which is going to be compared to the instance that is executing the method.
     * @return The boolean that represents the result of the equality check
     */
    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof IPAddressAndPort(InetAddress address_other, Integer port_other))) {
            return false;
        }

        return (address.equals(address_other) && port.equals(port_other));
    }
}
