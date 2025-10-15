import java.util.InputMismatchException;
import java.util.Scanner;

public class TestApplication {

    public static void main(String[] args) {
        // Declaração das variáveis
        short entityUCSAPId, entityPort, destinationUCSAPId;
        Scanner sc = new Scanner(System.in);
        UnicastServiceInterface unicastProtocol;
        String command, destinationMessage;
        boolean isRunning = true;

        System.out.println("Iniciando Aplicação de Testes...");
        System.out.println("");

        System.out.printf("Por favor, indique seu Identificador UCSAP: ");
        try {
            entityUCSAPId = sc.nextShort();
            sc.nextLine(); // Limpa o buffer

            System.out.printf("Por favor, indique sua Porta: ");
            entityPort = sc.nextShort();
            sc.nextLine(); // Limpa o buffer
        } catch (InputMismatchException ime) {
            System.err.println("Valor deve ser um número natural");
            sc.close();
            return;
        }
        System.out.println("");

        // Instanciação do UnicastProtocol
        unicastProtocol = new UnicastProtocol(
            entityUCSAPId,
            entityPort,
            new NotificationReceiver()
        );

        // Intanciação do thread receptor
        Thread receiverThread = new Thread((Runnable) unicastProtocol);
        receiverThread.start();

        System.out.println("Aplicação de Testes Iniciada");
        System.out.println("Para enviar mensagens digite SEND");
        System.out.println("Para sair digite EXIT");
        while (isRunning) {
            System.out.println("Esperando por notificações...");
            System.out.println("");

            command = sc.nextLine().toUpperCase();
            switch (command) {
                case "EXIT":
                    System.out.println("Saindo...");
                    isRunning = false;
                    break;
                case "SEND":
                    System.out.printf(
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
                    System.out.println("");

                    System.out.printf("Digite a Mensagem a ser enviada: ");

                    destinationMessage = sc.nextLine();
                    System.out.println("");

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
                    System.out.println("");
                    break;
                default:
                    System.out.println("Comando não reconhecido");
                    System.out.println("Para enviar mensagens digite SEND");
                    System.out.println("Para sair digite EXIT");
                    System.out.println("");
                    break;
            }
        }
        sc.close();
        try {
            receiverThread.interrupt();
        } catch (SecurityException se) {
            System.err.println(se);
        }
    }
}
