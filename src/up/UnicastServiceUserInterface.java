package up;

public interface UnicastServiceUserInterface {
    /**
     * Notifies the user when a message was received from unicast.UnicastProtocol.
     *
     * @param id The message's sender unicast.UnicastProtocol Identifier number
     * @param data A string containing the message data sent
     */
    void UPDataInd(short id, String data);
}
