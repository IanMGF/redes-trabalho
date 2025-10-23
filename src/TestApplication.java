import exceptions.InvalidFormatException;
import java.io.FileNotFoundException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.InputMismatchException;
import java.util.Scanner;

/**
 * @author Ian Marcos Gomes e Freitas
 * @author João Roberto de Moraes Neto
 *
 *
 */
class NotificationReceiver implements UnicastServiceUserInterface {

    /**
     * Notifies the user when a message was received from UnicastProtocol
     * by printing it on the screen.
     *
     * @params ucsapId The message's sender UnicastProtocol Identifier number
     * @params notification A string containing the message sent
     */
    @Override
    public void UPDataInd(short ucsapId, String notification) {
        System.out.println("Notificação Recebida de Identificador: " + ucsapId);
        System.out.println("Mensagem: " + notification);
        System.out.println();
    }
}

/**
 * @author Ian Marcos Gomes e Freitas
 * @author João Roberto de Moraes Neto
 *
 *
 */
public class TestApplication {

    static void main(String[] args) {
        // Declaração das variáveis
        boolean isRunning = true;
        short entityUCSAPId, entityPort, destinationUCSAPId;
        Scanner sc = new Scanner(System.in);
        UnicastServiceInterface unicastProtocol = null;
        String command, destinationMessage;

        System.out.println("Iniciando Aplicação de Testes...");
        System.out.println();

        System.out.print("Por favor, indique seu Identificador UCSAP: ");
        try {
            entityUCSAPId = sc.nextShort();
            sc.nextLine(); // Limpa o buffer

            System.out.print("Por favor, indique sua Porta: ");
            entityPort = sc.nextShort();
            sc.nextLine(); // Limpa o buffer
        } catch (InputMismatchException ime) {
            System.err.println(
                "Valor deve ser um número natural e menor que 65536"
            );
            sc.close();
            return;
        }
        System.out.println();

        // Instanciação do UnicastProtocol
        try {
            unicastProtocol = new UnicastProtocol(
                entityUCSAPId,
                entityPort,
                new NotificationReceiver()
            );
        } catch (IllegalArgumentException iae) {
            System.err.println(
                "Erro : Conjunto IP e Porta não foram encontrados no arquivo de configuração do Unicast"
            );
        } catch (FileNotFoundException fnfe) {
            System.err.println(
                "Erro : Não foi encontrado arquivo de configuração do Unicast"
            );
        } catch (SocketException se) {
            System.err.println(
                "Erro : Não foi possível inicar socket na porta: " + entityPort
            );
        } catch (UnknownHostException uhe) {
            System.err.println(
                "Erro : Endereço IP: " +
                    uhe +
                    "contido no arquivo de configuração do Unicast é inválido"
            );
        } catch (InvalidFormatException ife) {
            System.err.println(
                "Erro: Linhas do arquivo de configuração Unicast não seguem o formato <ucsapid> <host> <porta>"
            );
        }

        if (unicastProtocol == null) {
            sc.close();
            return;
        }

        System.out.println("Aplicação de Testes Iniciada");
        System.out.println("Para enviar mensagens digite SEND");
        System.out.println("Para sair digite EXIT");
        while (isRunning) {
            System.out.println("Esperando por notificações...");
            System.out.println();

            command = sc.nextLine().toUpperCase();
            switch (command) {
                case "EXIT":
                    System.out.println("Saindo...");
                    isRunning = false;
                    break;
                case "SEND":
                    System.out.print(
                        "Digite o Identificador UCSAP de destino: "
                    );

                    try {
                        destinationUCSAPId = sc.nextShort();
                    } catch (InputMismatchException ime) {
                        sc.nextLine(); // Limpa o buffer
                        System.err.println(
                            "Valor do identificador deve ser um número natural"
                        );
                        continue;
                    }
                    sc.nextLine(); // Limpa o buffer
                    System.out.println();

                    System.out.print("Digite a Mensagem a ser enviada: ");

                    destinationMessage = sc.nextLine();
                    System.out.println();

                    if (
                        unicastProtocol.UPDataReq(
                            destinationUCSAPId,
                            destinationMessage
                        )
                    ) {
                        System.out.println("Mensagem Enviada com Sucesso");
                    } else {
                        System.out.println("Erro ao Enviar a Mensagem");
                    }
                    System.out.println();
                    break;
                default:
                    System.out.println("Comando não reconhecido");
                    System.out.println("Para enviar mensagens digite SEND");
                    System.out.println("Para sair digite EXIT");
                    System.out.println();
                    break;
            }
        }
        sc.close();
    }
}
