package up;

import exceptions.InvalidFormatException;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class UnicastListener implements Runnable {

    private final DatagramSocket socket;
    private final UnicastServiceUserInterface userInterface;
    private final UnicastConfiguration configuration;
    private boolean running;

    UnicastListener(
            DatagramSocket socket,
            UnicastServiceUserInterface userInterface,
            UnicastConfiguration configuration
    ) {
        this.socket = socket;
        this.userInterface = userInterface;
        this.configuration = configuration;
        running = true;
    }

    @Override
    public void run() {
        DatagramPacket packet;

        while (running) {
            if (Thread.currentThread().isInterrupted()) {
                running = false;
            }

            byte[] receiveBuffer = new byte[1024];
            packet = new DatagramPacket(receiveBuffer, 1024);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            byte[] data = packet.getData();
            String dataStr = new String(data, StandardCharsets.UTF_8);
            String unpacked;
            try {
                unpacked = UnicastProtocol.UnpackData(dataStr);
            } catch (InvalidFormatException e) {
                continue;
            }

            InetAddress senderAddress = packet.getAddress();
            short senderPort = (short) packet.getPort();

            short UCSApId = configuration.GetId(
                    new IPAddressAndPort(senderAddress, senderPort)
            );

            userInterface.UPDataInd(UCSApId, unpacked);
        }
    }

    /**
     * Properly stop the listener
     */
    public void stop() {
        socket.close();
        running = false;
    }
}
