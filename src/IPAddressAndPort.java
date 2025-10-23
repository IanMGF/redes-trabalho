import java.net.InetAddress;

/**
 * @author Ian Marcos Gomes e Freitas
 * @author João Roberto de Moraes Neto
 *
 *
 */
public class IPAddressAndPort {
    public InetAddress address;
    public Short port;

    IPAddressAndPort(InetAddress address, short port) {
        this.address = address;
        this.port = port;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof IPAddressAndPort otherAddress)) {
            return false;
        }

        return address.equals(otherAddress.address) && port.equals(otherAddress.port);
    }
}