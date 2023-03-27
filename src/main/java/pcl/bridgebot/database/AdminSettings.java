package pcl.bridgebot.database;

public class AdminSettings {

    private String formatterMode;
    private String defaultWebhookName;

    private String defaultAdminChannel;

    public AdminSettings(String defaultAdminChannel) {
        this.defaultAdminChannel = defaultAdminChannel;
    }

    public String getDefaultAdminChannel() { return defaultAdminChannel; }

    public void setDefaultAdminChannel(String defaultAdminChannel) { this.defaultAdminChannel = defaultAdminChannel; }
}