public interface UnicastServiceInterface {
    /**
     * Sends a message to a node using the UnicastProtocol.
     *
     * @params id The message's receiver UnicastProtocol Identifier number.
     * @params data A string containing the message data sent.
     * @return The boolean that indicates if the message was successfully sent.
     */
    boolean UPDataReq(short id, String data);
}
