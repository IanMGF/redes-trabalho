import java.net.InetAddress;

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

        if (!(other instanceof IPAddressAndPort other_addr)) {
            return false;
        }

        return address.equals(other_addr.address) && port.equals(other_addr.port);
    }
}