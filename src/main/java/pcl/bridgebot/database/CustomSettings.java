package pcl.bridgebot.database;

public class CustomSettings {

    private String formatterMode;
    private String defaultWebhookName;

    public CustomSettings(String formatterMode, String defaultWebhookName) {
        this.formatterMode = formatterMode;
        this.defaultWebhookName = defaultWebhookName;
    }

    public String getFormatterMode() {
        return formatterMode;
    }

    public void setFormatterMode(String formatterMode) {
        this.formatterMode = formatterMode;
    }

    public String getDefaultWebhookName() {
        return defaultWebhookName;
    }

    public void setDefaultWebhookName(String defaultWebhookName) {
        this.defaultWebhookName = defaultWebhookName;
    }
}