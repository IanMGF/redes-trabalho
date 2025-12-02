package rip.node;

import rip.manager.RoutingManagementApplication;
import rip.manager.RoutingProtocolManagementInterface;
import rip.manager.RoutingProtocolManagementServiceUserInterface;

import java.util.Scanner;

public class RoutingNodeApplication {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int port;
        int nodeId;
        int timeoutMilliseconds;

        System.out.println("Iniciando aplicação da entidade nó...");
        System.out.print("Escolha um valor para o timeout da propagação periódica, em milissegundos (default=10000): ");

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

        System.out.print("Escolha o ID do nó: ");
        input = sc.nextLine();
        try {
            nodeId = Integer.parseInt(input);
        }  catch (NumberFormatException e) {
            System.err.println("Erro: o valor da porta deve ser um inteiro");
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

        RoutingInformationProtocol nodeEntityProtocol = new RoutingInformationProtocol((short) nodeId, (short) port);
        System.out.println("Aplicação da entidade nó iniciada");
    }
}
