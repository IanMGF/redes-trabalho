package up;

public interface UnicastServiceInterface {
    /**
     * Sends a message to a node using the unicast.UnicastProtocol.
     *
     * @param id The message's receiver Unicast Protocol Identifier number.
     * @param data A string containing the message data sent.
     * @return The boolean that indicates if the message was successfully sent.
     */
    boolean UPDataReq(short id, String data);
}
