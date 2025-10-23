public interface UnicastServiceUserInterface {
    /**
     * Notifies the user when a message was received from UnicastProtocol.
     *
     * @params id The message's sender UnicastProtocol Identifier number
     * @params data A string containing the message data sent
     */
    void UPDataInd(short id, String data);
}
