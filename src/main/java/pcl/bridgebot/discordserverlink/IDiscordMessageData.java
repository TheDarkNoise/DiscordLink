package pcl.bridgebot.discordserverlink;

public interface IDiscordMessageData {
    public String getDiscordChannelId();
    
    public String getUserId();
    
    public String getUserName();

    public String getMessage();
}