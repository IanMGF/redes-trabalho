package rip.operations;

public class RoutingInformationProtocolRequest extends RoutingInformationProtocolOperation {
    public static RoutingInformationProtocolRequest parse(String data) {
        if (data.equals(RoutingInformationProtocolOperationType.REQUEST.toString()))
            return new RoutingInformationProtocolRequest();
        else {
            return null;
        }
    }

    @Override
    public String toString() {
        return RoutingInformationProtocolOperationType.REQUEST.toString();
    }
}
