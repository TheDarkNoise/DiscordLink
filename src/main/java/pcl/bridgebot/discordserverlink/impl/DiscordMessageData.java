package pcl.bridgebot.discordserverlink.impl;

import net.dv8tion.jda.api.entities.Message;
import pcl.bridgebot.discordserverlink.IDiscordMessageData;

public class DiscordMessageData implements IDiscordMessageData {
    private final String userId;
    private final String userName;
    private final String message;

    private final String rawMessage;

    private final String discordChannelId;

    private final String messageID;

    private final Message discordMessage;

    public DiscordMessageData(String discordChannelId, String userId, String userName, String message, String rawMessage, String messageID, Message discordMessage) {
        this.discordChannelId = discordChannelId;
        this.userId = userId;
        this.userName = userName;
        this.message = message;
        this.rawMessage = rawMessage;
        this.messageID = messageID;
        this.discordMessage = discordMessage;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public String getUserName() {
        return userName;
    }

    @Override
    public String getDiscordChannelId() {
        return discordChannelId;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public String getRawMessage() { return rawMessage; }

    @Override
    public String getMessageId() { return messageID; }
    @Override
    public Message getDiscordMessage() { return discordMessage; }

}