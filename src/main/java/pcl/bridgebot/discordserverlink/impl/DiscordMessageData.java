package pcl.bridgebot.discordserverlink.impl;

import pcl.bridgebot.discordserverlink.IDiscordMessageData;

public class DiscordMessageData implements IDiscordMessageData {
    private final String userId;
    private final String userName;
    private final String message;
    private final String discordChannelId;

    public DiscordMessageData(String discordChannelId, String userId, String userName, String message) {
        this.discordChannelId = discordChannelId;
        this.userId = userId;
        this.userName = userName;
        this.message = message;
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
}