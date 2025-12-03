import exceptions.InvalidFormatException;
import exceptions.InvalidPortException;
import up.UnicastProtocol;
import up.UnicastServiceInterface;

import java.io.FileNotFoundException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

/**
 * @author Ian Marcos Gomes e Freitas
 * @author João Roberto de Moraes Neto
 *
 *
 */
public class TestApplication {
    public static void main(String[] args) {
        // Declaração das variáveis
        boolean isRunning = true;
        short entityUCSAPId, destinationUCSAPId;
        int entityPort;
        Scanner sc = new Scanner(System.in);
        UnicastServiceInterface unicastProtocol = null;
        String command, destinationMessage;

        System.out.println("Iniciando Aplicação de Testes...");
        System.out.println();

        try {
            System.out.print("Por favor, indique seu Identificador UCSAP: ");
            entityUCSAPId = Short.parseShort(sc.nextLine());
            if (entityUCSAPId < 0) {
                System.err.println("Valor deve ser um número natural e menor que 32768");
                sc.close();
                return;
            }
        } catch (NumberFormatException nfe) {
            System.err.println("Valor deve ser um número natural e menor que 32768");
            sc.close();
            return;
        }
        try {
            System.out.print("Por favor, indique sua Porta: ");
            entityPort = Integer.parseInt(sc.nextLine());
            if (entityPort < 0 || entityPort >= 65536) {
                System.err.println("Valor deve ser uma porta válida (0-65535)");
                sc.close();
                return;
            }
        } catch (NumberFormatException nfe) {
            System.err.println("Valor deve ser uma porta válida (0-65535)");
            sc.close();
            return;
        }
        System.out.println();

        // Instanciação do UnicastProtocol
        TestMessageReceiver messageReceiver = new TestMessageReceiver();
        try {
            unicastProtocol = new UnicastProtocol(entityUCSAPId, entityPort, messageReceiver);
        } catch (IllegalArgumentException iae) {
            System.err.println("Erro : Conjunto ID e Porta não foram encontrados no arquivo de configuração do Unicast");
        } catch (FileNotFoundException fnfe) {
            System.err.println("Erro : Não foi encontrado arquivo de configuração do Unicast ('unicast.conf')");
        } catch (SocketException se) {
            System.err.printf("Erro : Não foi possível inicar atrelar socket à porta %s\n", entityPort);
        } catch (UnknownHostException uhe) {
            System.err.printf("Erro : Endereço IP: '%s' contido no arquivo de configuração do Unicast é inválido\n", uhe);
        } catch (InvalidFormatException ife) {
            System.err.printf("Erro: Linha do arquivo de configuração Unicast não seguem o formato <ucsapid> <host> <porta>: '%s'\n", ife.getText());
        } catch (InvalidPortException ipe) {
            System.err.printf("Erro: Porta inválida encontrada no arquivo de configuração: %s. Portas válidas: 1025-65535\n", ipe.getPort());
        }

        if (unicastProtocol == null) {
            sc.close();
            return;
        }

        System.out.println("Aplicação de Testes Iniciada");
        System.out.println("Mensagens recebidas serão apresentadas\n");

        System.out.println("Para enviar mensagens digite SEND");
        System.out.println("Para sair digite EXIT\n");


        while (isRunning) {

            command = sc.nextLine().toUpperCase();
            switch (command) {
                case "EXIT":
                    System.out.println("Saindo...");
                    isRunning = false;
                    break;
                case "SEND":
                    System.out.print("Digite o Identificador UCSAP de destino: ");

                    try {
                        String line = sc.nextLine();
                        destinationUCSAPId = Short.parseShort(line);
                        if(destinationUCSAPId < 0) {
                            System.err.println("Valor do identificador deve ser um número natural");
                            continue;
                        }
                    } catch (NumberFormatException ime) {
                        System.err.println("Valor do identificador deve ser um número natural");
                        continue;
                    }
                    System.out.print("Digite a Mensagem a ser enviada: ");

                    destinationMessage = sc.nextLine();
                    System.out.println();

                    boolean dataReqSuccessful = unicastProtocol.UPDataReq(destinationUCSAPId, destinationMessage);
                    if (dataReqSuccessful) {
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
        // Despite the possibility to use `protocol` as an instance of UnicastProtocol,
        // this is the only time the specific implementation matters
        @SuppressWarnings("CastCanBeRemovedNarrowingVariableType")
        UnicastProtocol protocol = (UnicastProtocol) unicastProtocol;
        protocol.stop();
    }
}
