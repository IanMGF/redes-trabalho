import up.UnicastServiceUserInterface;

/**
 * @author Ian Marcos Gomes e Freitas
 * @author João Roberto de Moraes Neto
 *
 *
 */
public class TestMessageReceiver implements UnicastServiceUserInterface {

    /**
     * Notifies the user when a message was received from unicast.UnicastProtocol
     * by printing it on the screen.
     *
     * @param UCSApId The message's sender unicast.UnicastProtocol Identifier number
     * @param message A string containing the message sent
     */
    @Override
    public void UPDataInd(short UCSApId, String message) {
        System.out.println("Notificação Recebida!");
        System.out.println("Identificador: " + UCSApId);
        System.out.println("Mensagem: " + message);
        System.out.println();
    }
}
