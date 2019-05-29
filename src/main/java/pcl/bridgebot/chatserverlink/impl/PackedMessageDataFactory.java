package pcl.bridgebot.chatserverlink.impl;

import pcl.bridgebot.chatserverlink.IPackedMessageData;
import pcl.bridgebot.chatserverlink.IPackedMessageDataFactory;
import pcl.bridgebot.chatserverlink.InvalidPackedMessageException;

public class PackedMessageDataFactory implements IPackedMessageDataFactory {
    @Override
    public IPackedMessageData getPackedMessageData(String chatroom, int userId, String userNickname, String message) {
        return new PackedMessageData(chatroom, userId, userNickname, message);
    }

    @Override
    public IPackedMessageData getPackedMessageData(byte[] packetMessage, int announcedSize)
            throws InvalidPackedMessageException {
        return PackedMessageData.getDataFromPackedMessage(packetMessage, announcedSize);
    }
}
