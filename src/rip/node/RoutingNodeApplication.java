package rip.node;

import java.util.Scanner;

public class RoutingNodeApplication {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        RoutingInformationProtocol a = new RoutingInformationProtocol((short) 1, (short) 1025);
        RoutingInformationProtocol b = new RoutingInformationProtocol((short) 2, (short) 1026);
        RoutingInformationProtocol c = new RoutingInformationProtocol((short) 3, (short) 1027);
        RoutingInformationProtocol d = new RoutingInformationProtocol((short) 4, (short) 1028);
        RoutingInformationProtocol e = new RoutingInformationProtocol((short) 5, (short) 1029);
        RoutingInformationProtocol f = new RoutingInformationProtocol((short) 6, (short) 1030);
    }
}
