package pcl.bridgebot.chatserverlink;

public interface IPackedMessageDataFactory {

    /**
     * Gets a packed message data that can be sent to the server using a series of
     * parameters
     * 
     * @param chatroom
     * @param userId
     * @param userNickname
     * @param message
     * @return
     * @throws InvalidPackedMessageException
     */
    public IPackedMessageData getPackedMessageData(String chatroom, int userId, String userNickname, String message);

    /**
     * Gets a packed message data that can be read via an API using data received
     * from the server
     * 
     * @param packetMessage
     * @param announcedSize
     * @return
     * @throws InvalidPackedMessageException
     */
    public IPackedMessageData getPackedMessageData(byte[] packetMessage, int announcedSize)
            throws InvalidPackedMessageException;
}