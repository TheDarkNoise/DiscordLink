package pcl.bridgebot.chatserverlink.impl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import pcl.bridgebot.chatserverlink.IPackedMessageData;
import pcl.bridgebot.chatserverlink.InvalidPackedMessageException;
import pcl.bridgebot.chatserverlink.MessageTypeEnum;

public class PackedMessageData implements IPackedMessageData {
    private int userId;
    private String chatroom;
    private String message;
    private String userNickname;
    private MessageTypeEnum messageType;

    private PackedMessageData() {
    }

    public PackedMessageData(String chatroom, int userId, String userNickname, String message) {
        this.chatroom = chatroom;
        this.userId = userId;
        this.userNickname = userNickname;
        this.message = message;
        this.messageType = MessageTypeEnum.CHANNEL;
    }

    @Override
    public byte[] getPackedMessage() throws InvalidPackedMessageException {
        if (messageType == MessageTypeEnum.MOTD) {
            throw new IllegalArgumentException();
        }
        // Standard Client=>Server Packed message format :
        // - Int16 Packet size.
        // - Int8 Message type; 0x64 is "Send to Channel".
        // - Int32 User ID.
        // - PascalStr (Int8 + Char[]) Chat room name
        // - PascalStr (Int8 + Char[]) Message
        // - PascalStr (Int8 + Char[]) Nickname (ignored by the server)

        int messageSize = 2 + 1 + 4 // Message head (packet size, message type, user ID)
                + chatroom.getBytes(StandardCharsets.UTF_8).length + 1 // Pascal string for chatroom
                + message.getBytes(StandardCharsets.UTF_8).length + 1 // Pascal string for message
                + userNickname.getBytes(StandardCharsets.UTF_8).length + 1; // Pascal string for nickname
        ByteBuffer buffer = ByteBuffer.allocate(messageSize);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        // Add the packet size
        buffer.putShort((short) messageSize);
        // Add the MESSAGE_TYPE value (0x64)
        buffer.put((byte) 0x64);
        // Add the User ID
        buffer.putInt(userId);
        // Add the text
        writePascalStringToBuffer(buffer, chatroom);
        writePascalStringToBuffer(buffer, message);
        writePascalStringToBuffer(buffer, userNickname);
        return buffer.array();
    }

    private static void writePascalStringToBuffer(ByteBuffer buffer, String data) {
        byte[] dataToSend = data.getBytes(StandardCharsets.UTF_8);
        buffer.put((byte) dataToSend.length);
        buffer.put(dataToSend);
    }

    @Override
    public String getChatroom() {
        return chatroom;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public int getUserId() {
        return userId;
    }

    @Override
    public String getUserNickname() {
        return userNickname;
    }

    @Override
    public MessageTypeEnum getMessageType() {
        return messageType;
    }

    public static PackedMessageData getDataFromPackedMessage(byte[] packedMessage, int announcedSize)
            throws InvalidPackedMessageException {

        ByteBuffer buffer = ByteBuffer.wrap(packedMessage);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // Get packet size (for checking)
        if (buffer.remaining() < 2)
            throw new InvalidPackedMessageException("No buffer content for packet size", buffer.position());

        short packetSize = buffer.getShort();
        if (packetSize != announcedSize)
            throw new InvalidPackedMessageException(String
                    .format("Stream size %04X differs from header announced size %04X", announcedSize, packetSize),
                    buffer.position());

        PackedMessageData result = new PackedMessageData();
        if (buffer.remaining() < 1)
            throw new InvalidPackedMessageException("No buffer content for message type", buffer.position());
        byte messageTypeId = buffer.get();
        if (messageTypeId == 0x64) {
            // Standard Sever=>Client Packed message format :
            // - Int16 Packet size
            // - Int8 Message type; 0x64 is "Send to Channel"
            // - Int32 User ID.
            // - PascalStr (Int8 + Char[]) Chat room name
            // - PascalStr (Int8 + Char[]) Message
            // - PascalStr (Int8 + Char[]) Nickname

            result.messageType = MessageTypeEnum.CHANNEL;

            if (buffer.remaining() < 4)
                throw new InvalidPackedMessageException("No buffer content for user ID", buffer.position());
            result.userId = buffer.getInt();

            // Pascal string for chatroom
            result.chatroom = getPascalStringFromBuffer(buffer, "ChatRoom");
            // Pascal string for message
            result.message = getPascalStringFromBuffer(buffer, "Message");
            // Pascal string for nickname
            result.userNickname = getPascalStringFromBuffer(buffer, "Username");
            if (buffer.position() != packetSize)
                throw new InvalidPackedMessageException(
                        String.format("End packet byte position differs from announced size %04X", packetSize),
                        buffer.position());
            return result;
        }

        if (messageTypeId == 0x65) {
            // MOTD Sever=>Client Packed message format :
            // - Int16 Packet size
            // - Int8 Message type; 0x65 is "Message of the Day"
            // - PascalStr (Int8 + Char[]) Chat room name
            // - PascalStr (Int8 + Char[]) Message

            result.messageType = MessageTypeEnum.MOTD;
            // Pascal string for chatroom
            result.chatroom = getPascalStringFromBuffer(buffer, "ChatRoom");
            // Pascal string for message
            result.message = getPascalStringFromBuffer(buffer, "Message");
            result.userNickname = new String("MOTD Update");
            if (buffer.position() != packetSize)
                throw new InvalidPackedMessageException(
                        String.format("End packet byte position differs from announced size %04X", packetSize),
                        buffer.position());
            return result;
        }

        throw new InvalidPackedMessageException(String.format("Unknown message type %02X", messageTypeId),
                buffer.position());
    }

    private static String getPascalStringFromBuffer(ByteBuffer buffer, String stringName)
            throws InvalidPackedMessageException {
        if (buffer.remaining() < 1)
            throw new InvalidPackedMessageException(String.format("No header for string %s", stringName),
                    buffer.position());
        int stringLength = (int) (buffer.get() & ((int) 0xff));
        if (stringLength < 0)
            throw new InvalidPackedMessageException(
                    String.format("String %s has a negative advertized size", stringName), buffer.position());
        byte[] rawStringBytes = new byte[stringLength];
        if (buffer.remaining() < stringLength)
            throw new InvalidPackedMessageException(
                    String.format("Not enough remaining buffer for string %s", stringName), buffer.position());
        buffer.get(rawStringBytes);
        return new String(rawStringBytes, StandardCharsets.UTF_8);
    }
}