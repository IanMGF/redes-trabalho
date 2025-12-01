package rip.operations;

public class RoutingInformationProtocolOperationParser {
    public static RoutingInformationProtocolOperation parse(String data_unit) {
        String[] pieces = data_unit.split(" ");
        if (pieces.length < 1) return null;

        RoutingInformationProtocolOperationType type = RoutingInformationProtocolOperationType.fromString(pieces[0]);
        if (type == null) return null;

        switch (type) {
            case GET -> {
                return RoutingInformationProtocolGet.parse(data_unit);
            }
            case SET -> {
                return RoutingInformationProtocolSet.parse(data_unit);
            }
            case NOTIFICATION -> {
                return RoutingInformationProtocolNotification.parse(data_unit);
            }
            case INDICATION -> {
                return RoutingInformationProtocolIndication.parse(data_unit);
            }
            case REQUEST -> {
                return RoutingInformationProtocolRequest.parse(data_unit);
            }
            case RESPONSE -> {
                return RoutingInformationProtocolResponse.parse(data_unit);
            }
            default -> {
                return null;
            }
        }
    }
}
