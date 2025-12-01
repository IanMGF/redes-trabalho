package rip.manager;

import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

public class RoutingManagementApplication
    implements RoutingProtocolManagementServiceUserInterface {
    private static final Semaphore waiting_response = new Semaphore(0);

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int port;
        int timeoutMilliseconds;
        String command;
        boolean isRunning = true;

        System.out.println("Iniciando aplicação da entidade gerente...");
        System.out.print("Escolha um valor para o timeout de requisições em milisegundos (default=10000): ");

        String input = sc.nextLine();
        if (input.isEmpty()) {
            timeoutMilliseconds = 10000;
        } else {
            try {
                timeoutMilliseconds = Integer.parseInt(input);
            }  catch (NumberFormatException e) {
                System.err.println("Erro: o valor em milissegundos do timeout deve ser um inteiro");
                return;
            }
        }

        if (timeoutMilliseconds <= 0) {
            System.err.println("Erro: o valor em milissegundos do timeout deve ser um inteiro maior que zero");
            return;
        }

        System.out.print("Escolha um valor para a porta utilizada: ");
        input = sc.nextLine();
        try {
            port = Integer.parseInt(input);
        }  catch (NumberFormatException e) {
            System.err.println("Erro: o valor da porta deve ser um inteiro");
            return;
        }

        RoutingProtocolManagementServiceUserInterface managementApplication = new RoutingManagementApplication();
        RoutingProtocolManagementInterface ripInterface = null;
        try {
            ripInterface = new RoutingInformationProtocol(managementApplication, timeoutMilliseconds, port);
        } catch (IllegalStateException e) {
            return;
        }

        System.out.println("Aplicação da entidade gerente iniciada");
        printCommands();

        while (isRunning){
            short firstNodeId, secondNodeId;
            int cost;
            command = sc.nextLine().toUpperCase().strip();

            switch (command) {
                case "GET-TABLE":
                    System.out.print("Digite o ID do nó a ser requisitado: ");
                    try {
                        firstNodeId = sc.nextShort();
                    } catch (InputMismatchException e) {
                        System.err.println("Erro: o valor do ID deve ser um inteiro");
                        break;
                    }

                    if(ripInterface.getDistanceTable(firstNodeId)){
                        System.out.printf("Requisição de tabela de distância para o nó (%d) realizada\n", firstNodeId);
                        System.out.println("Esperando resposta...");

                        try{
                            waiting_response.acquire();
                        } catch (InterruptedException e) {
                            System.err.println(e.getMessage());
                        }

                    } else {
                        System.err.println("Erro: o ID digitado não é válido");
                    }
                    break;
                case "GET-COST":
                    try {
                        System.out.print("Digite o ID do primeiro nó do enlace (o que será requisitado): ");
                        firstNodeId = sc.nextShort();
                        System.out.print("Digite o ID do segundo nó do enlace: ");
                        secondNodeId = sc.nextShort();
                    } catch (InputMismatchException e) {
                        System.err.println("Erro: o valor do ID deve ser um inteiro");
                        break;
                    }

                    if(ripInterface.getLinkCost(firstNodeId, secondNodeId)){
                        System.out.printf("Requisição do custo de enlace entre nó (%d) e nó (%d) enviada para nó (%d)\n", firstNodeId, secondNodeId, firstNodeId);
                        System.out.println("Esperando resposta...");

                        try{
                            waiting_response.acquire();
                        } catch (InterruptedException e) {
                            System.err.println(e.getMessage());
                        }

                    } else {
                        System.err.println("Erro: o(s) ID(s) digitados não são válidos");
                    }
                    break;
                case "SET-COST":
                    try {
                        System.out.print("Digite o ID do primeiro nó do enlace: ");
                        firstNodeId = sc.nextShort();
                        System.out.print("Digite o ID do segundo nó do enlace: ");
                        secondNodeId = sc.nextShort();
                        System.out.print("Digite o novo custo para o enlace: ");
                        cost = sc.nextInt();
                    } catch (InputMismatchException e) {
                        System.err.println("Erro: o valor do ID e dos custos devem ser inteiros");
                        break;
                    }

                    if(ripInterface.setLinkCost(firstNodeId, secondNodeId, cost)){
                        System.out.printf("Requisição de alteração do custo de enlace entre nó (%d) e nó (%d) para %d enviada\n", firstNodeId, secondNodeId, cost);
                        System.out.println("Esperando resposta...");

                        try{
                            waiting_response.acquire();
                        } catch (InterruptedException e) {
                            System.err.println(e.getMessage());
                        }

                    } else {
                        System.err.println("Erro: o(s) ID(s) e/ou o custo digitados não são válidos");
                    }
                    break;
                case "EXIT":
                    System.out.println("Saindo...");
                    isRunning = false;
                    sc.close();
                    continue;
                case "":
                    continue;
                default:
                    System.out.println("Comando não reconhecido");
                    continue;
            }

            printCommands();
        }

    }

    private static void printCommands(){
        System.out.println("\nDigite um dos comandos possíveis");
        System.out.println("(GET-TABLE) - Requisitar a tabela de distância de um nó.");
        System.out.println("(GET-COST) - Requisitar o custo do enlace conectando dois nós.");
        System.out.println("(SET-COST) - Redefinir o custo do enlace conectando dois nós");
        System.out.println("(EXIT) - Encerrar aplicação de gerência");
        System.out.print("Comando: ");
    }

    @Override
    public void distanceTableIndication(short nodeId, int[][] distanceTable) {
        waiting_response.release();
        System.out.printf("\nTabela de distância recebida do nó (%d)\n", nodeId);
        for (int[] row : distanceTable) {
            for (int col : row) {
                System.out.print(col + " ");
            }
            System.out.println();
        }
    }

    @Override
    public void linkCostIndication(
        short firstNodeId,
        short secondNodeId,
        int linkCost
    ) {
        waiting_response.release();
        System.out.printf("\nCusto do enlace entre nó (%d) e nó (%d) recebido = %d\n", firstNodeId, secondNodeId, linkCost);
    }
}
