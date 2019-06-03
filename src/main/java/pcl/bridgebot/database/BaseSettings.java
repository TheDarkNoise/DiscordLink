package pcl.bridgebot.database;

public class BaseSettings {
    // ChatServer informations
    private final String chatserverIP;
    private final int chatserverport;
    private final String defaultGID;

    // Discord informations
    private final String discordToken;

    // HTTPd informations
    private final int httpdPort;
    private final String httpdSecret;

    public BaseSettings(String chatserverIP, int chatserverport, String defaultGID, String discordToken, int httpdPort,
            String httpdSecret) {
        this.chatserverIP = chatserverIP;
        this.chatserverport = chatserverport;
        this.defaultGID = defaultGID;
        this.discordToken = discordToken;
        this.httpdPort = httpdPort;
        this.httpdSecret = httpdSecret;
    }

    public String getChatserverIP() {
        return chatserverIP;
    }

    public int getChatserverport() {
        return chatserverport;
    }

    public String getDefaultGID() {
        return defaultGID;
    }

    public String getDiscordToken() {
        return discordToken;
    }

    public int getHttpdPort() {
        return httpdPort;
    }

    public String getHttpdSecret() {
        return httpdSecret;
    }
}