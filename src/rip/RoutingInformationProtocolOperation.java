package rip;

public enum RoutingInformationProtocolOperation {
    RIPGET,
    RIPSET,
    RIPNTF,
    RIPIND,
    RIPRQT,
    RIPRSP;

    public String toString() {
        switch(this) {
            case RIPGET -> {
                return "RIPGET";
            }
            case RIPSET -> {
                return "RIPSET";
            }
            case RIPNTF -> {
                return "RIPNTF";
            }
            case RIPIND -> {
                return "RIPIND";
            }
            case RIPRQT -> {
                return "RIPRQT";
            }
            case RIPRSP -> {
                return "RIPRSP";
            }
            default -> {
                return null;
            }
        }
    }
}
