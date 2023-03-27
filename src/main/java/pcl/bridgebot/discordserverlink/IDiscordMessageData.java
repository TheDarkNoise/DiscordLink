package pcl.bridgebot.discordserverlink;

import net.dv8tion.jda.api.entities.Message;

public interface IDiscordMessageData {
    public String getDiscordChannelId();
    
    public String getUserId();
    
    public String getUserName();

    public String getMessage();

    public String getRawMessage();

    public String getMessageId();

    Message getDiscordMessage();
}