package pcl.bridgebot.chatserverlink;

public interface IPackedMessageData {

    public byte[] getPackedMessage() throws InvalidPackedMessageException;

    public String getChatroom();

    public String getMessage();

    public int getUserId();

    public String getUserNickname();

    public MessageTypeEnum getMessageType();
}