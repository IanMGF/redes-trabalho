package rip.operations;

public enum RoutingInformationProtocolOperationType {
    GET,
    SET,
    NOTIFICATION,
    INDICATION,
    REQUEST,
    RESPONSE;

    public String toString() {
        switch(this) {
            case GET -> {
                return "RIPGET";
            }
            case SET -> {
                return "RIPSET";
            }
            case NOTIFICATION -> {
                return "RIPNTF";
            }
            case INDICATION -> {
                return "RIPIND";
            }
            case REQUEST -> {
                return "RIPRQT";
            }
            case RESPONSE -> {
                return "RIPRSP";
            }
            default -> {
                return null;
            }
        }
    }

    public static RoutingInformationProtocolOperationType fromString(String ind) {
        switch(ind) {
            case "RIPGET"-> {
                return GET;
            }
            case "RIPSET"-> {
                return SET;
            }
            case "RIPNTF"-> {
                return NOTIFICATION;
            }
            case "RIPIND"-> {
                return INDICATION;
            }
            case "RIPRQT"-> {
                return REQUEST;
            }
            case "RIPRSP"-> {
                return RESPONSE;
            }
            default -> {
                return null;
            }
        }
    }
}
