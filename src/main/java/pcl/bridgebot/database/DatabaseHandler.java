package pcl.bridgebot.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

public class DatabaseHandler {

    public DatabaseHandler() {
    }

    public static boolean initialize() throws SQLException {
        Database.init();

        // Create the tables : Channels, UserMap, Config, JsonData
        Database.addStatement("CREATE TABLE IF NOT EXISTS Channels(globalName, discordID PRIMARY KEY)");
        Database.addStatement("CREATE TABLE IF NOT EXISTS UserMap(globalID PRIMARY KEY, discordID)");
        Database.addStatement("CREATE TABLE IF NOT EXISTS Config(key PRIMARY KEY, data)");
        Database.addStatement(
                "CREATE TABLE IF NOT EXISTS JsonData (mykey VARCHAR(255) PRIMARY KEY NOT NULL, store TEXT DEFAULT NULL);");

        // Channels
        Database.addPreparedStatement("addChannel", "REPLACE INTO Channels (globalName, discordID) VALUES (?,?);");
        Database.addPreparedStatement("removeChannel", "DELETE FROM Channels WHERE discordID = ?;");
        Database.addPreparedStatement("getChannelByGlobal", "SELECT * FROM Channels WHERE globalName = ?;");
        Database.addPreparedStatement("getChannelByDiscordID", "SELECT * FROM Channels WHERE discordID = ?;");
        Database.addPreparedStatement("getAllChannels", "SELECT * FROM Channels;");

        // UserMap
        Database.addPreparedStatement("getUserByGlobal", "SELECT discordID FROM UserMap WHERE globalID = ?;");
        Database.addPreparedStatement("getUserByDiscordID", "SELECT globalID FROM UserMap WHERE discordID = ?;");
        Database.addPreparedStatement("addUser", "REPLACE INTO UserMap (globalID, discordID) VALUES (?,?);");
        Database.addPreparedStatement("delUser", "DELETE FROM UserMap WHERE discordID = ?;");
        Database.addPreparedStatement("getAllUsers", "SELECT globalID, discordID FROM UserMap;");

        // JSONStorage
        Database.addPreparedStatement("storeJSON", "INSERT OR REPLACE INTO JsonData (mykey, store) VALUES (?, ?);");
        Database.addPreparedStatement("retreiveJSON", "SELECT store FROM JsonData WHERE mykey = ?");

        // Default config stuff
        Database.addPreparedStatement("getSettings", "SELECT data FROM Config WHERE key = ? LIMIT 1;");

        Database.addPreparedStatement("setSettings", "REPLACE INTO Config (key, data) VALUES (?,?);");
        return true;
    }

    public Optional<BaseSettings> getSettings() {
        try {
            PreparedStatement getSettings = Database.getPreparedStatement("getSettings");

            getSettings.setString(1, "chatserver");
            ResultSet chatServerQueryResult = getSettings.executeQuery();
            if (!chatServerQueryResult.next())
                return Optional.empty();
            String chatserverIp = chatServerQueryResult.getString(1);

            getSettings.setString(1, "chatserverport");
            ResultSet chatServerPortQueryResult = getSettings.executeQuery();
            if (!chatServerPortQueryResult.next())
                return Optional.empty();
            int chatserverport = chatServerPortQueryResult.getInt(1);

            getSettings.setString(1, "httpdport");
            ResultSet httpdPortQueryResult = getSettings.executeQuery();
            if (!httpdPortQueryResult.next())
                return Optional.empty();
            int httpdPort = httpdPortQueryResult.getInt(1);

            getSettings.setString(1, "httpdsecret");
            ResultSet httpdSecretQueryResult = getSettings.executeQuery();
            if (!httpdSecretQueryResult.next())
                return Optional.empty();
            String httpdSecret = httpdSecretQueryResult.getString(1);

            getSettings.setString(1, "defaultGID");
            ResultSet defaultGIDQueryResult = getSettings.executeQuery();
            if (!defaultGIDQueryResult.next())
                return Optional.empty();
            String defaultGID = defaultGIDQueryResult.getString(1);

            getSettings.setString(1, "discordToken");
            ResultSet discordTokenQueryResult = getSettings.executeQuery();
            if (!discordTokenQueryResult.next())
                return Optional.empty();
            String discordToken = discordTokenQueryResult.getString(1);

            return Optional.of(
                    new BaseSettings(chatserverIp, chatserverport, defaultGID, discordToken, httpdPort, httpdSecret));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public void setSettings(BaseSettings settings) throws Exception, SQLException {
        PreparedStatement setSettings = Database.getPreparedStatement("setSettings");
        setSettings.setString(1, "discordToken");
        setSettings.setString(2, settings.getDiscordToken());
        setSettings.execute();

        setSettings.setString(1, "defaultGID");
        setSettings.setString(2, settings.getDefaultGID());
        setSettings.execute();

        setSettings.setString(1, "chatserver");
        setSettings.setString(2, settings.getChatserverIP());
        setSettings.execute();

        setSettings.setString(1, "httpdport");
        setSettings.setString(2, Integer.toString(settings.getHttpdPort()));
        setSettings.execute();

        setSettings.setString(1, "httpdsecret");
        setSettings.setString(2, settings.getHttpdSecret());
        setSettings.execute();

        setSettings.setString(1, "chatserverport");
        setSettings.setString(2, Integer.toString(settings.getChatserverport()));
        setSettings.execute();
    }

    public CustomSettings getCustomSettings() {
        String formatterMode = "InGame";
        String defaultWebhookName = "GlobalChat";
        try {
            PreparedStatement getSettings = Database.getPreparedStatement("getSettings");

            getSettings.setString(1, "formatterMode");
            ResultSet formatterModeQueryResult = getSettings.executeQuery();
            if (formatterModeQueryResult.next())
                formatterMode = formatterModeQueryResult.getString(1);

            getSettings.setString(1, "defaultWebhookName");
            ResultSet defaultWebhookNameQueryResult = getSettings.executeQuery();
            if (defaultWebhookNameQueryResult.next())
                defaultWebhookName = defaultWebhookNameQueryResult.getString(1);

        } catch (Exception e) {
            // NO OP, just use default parameters
        }
        return new CustomSettings(formatterMode, defaultWebhookName);
    }

    public AdminSettings getAdminSettings() {
        String defaultAdminChannel = "000000000000";
        try {
            PreparedStatement getSettings = Database.getPreparedStatement("getSettings");

            getSettings.setString(1, "defaultAdminChannel");
            ResultSet defaultAdminChannelQueryResult = getSettings.executeQuery();
            if (defaultAdminChannelQueryResult.next())
                defaultAdminChannel = defaultAdminChannelQueryResult.getString(1);

        } catch (Exception e) {
            // NO OP, just use default parameters
        }
        return new AdminSettings(defaultAdminChannel);
    }

    public void setCustomSettings(CustomSettings settings) throws Exception, SQLException {
        PreparedStatement setSettings = Database.getPreparedStatement("setSettings");

        setSettings.setString(1, "formatterMode");
        setSettings.setString(2, settings.getFormatterMode());
        setSettings.execute();

        setSettings.setString(1, "defaultWebhookName");
        setSettings.setString(2, settings.getDefaultWebhookName());
        setSettings.execute();
    }


    public void setAdminSettings(AdminSettings settings) throws Exception, SQLException {
        PreparedStatement setSettings = Database.getPreparedStatement("setSettings");

        setSettings.setString(1, "defaultAdminChannel");
        setSettings.setString(2, settings.getDefaultAdminChannel());
        setSettings.execute();
    }

    public Collection<String> getGameChannelListFromDiscord(String discordID) {
        try {
            PreparedStatement getChannelByDiscordID = Database.getPreparedStatement("getChannelByDiscordID");
            getChannelByDiscordID.setString(1, discordID);
            ResultSet results = getChannelByDiscordID.executeQuery();
            ArrayList<String> result = new ArrayList<>();
            while (results.next()) {
                result.add(results.getString(1));
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public Collection<String> getDiscordChannelListFromGame(String channelId) {
        try {
            PreparedStatement getChannelByGlobal = Database.getPreparedStatement("getChannelByGlobal");
            getChannelByGlobal.setString(1, channelId);
            ResultSet results = getChannelByGlobal.executeQuery();
            ArrayList<String> result = new ArrayList<>();
            while (results.next()) {
                result.add(results.getString(2));
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public Optional<Integer> getCharacterId(String discordId) {
        try {
            PreparedStatement getUserByDiscordID = Database.getPreparedStatement("getUserByDiscordID");
            getUserByDiscordID.setString(1, discordId);
            ResultSet results = getUserByDiscordID.executeQuery();
            if (results.next()) {
                return Optional.of(Integer.valueOf(results.getString(1)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public Optional<String> getDiscordId(int userId) {
        try {
            PreparedStatement getUserByDiscordID = Database.getPreparedStatement("getUserByGlobal");
            getUserByDiscordID.setString(1, Integer.toString(userId));
            ResultSet results = getUserByDiscordID.executeQuery();
            if (results.next()) {
                return Optional.of(results.getString(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public boolean handleAddChannelAction(String channelName, String discordId) {
        try {
            PreparedStatement addChannel = Database.getPreparedStatement("addChannel");
            addChannel.setString(1, channelName);
            addChannel.setString(2, discordId);
            addChannel.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean handleRemoveChannelAction(String discordId) {
        try {
            PreparedStatement delChannel = Database.getPreparedStatement("removeChannel");
            delChannel.setString(1, discordId);
            delChannel.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean handleAddUserAction(String inGameId, String discordId) {
        try {
            PreparedStatement addUser = Database.getPreparedStatement("addUser");
            addUser.setString(1, inGameId);
            addUser.setString(2, discordId);
            addUser.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean handleRemoveUserAction(String discordId) {
        try {
            PreparedStatement delUser = Database.getPreparedStatement("delUser");
            delUser.setString(1, discordId);
            delUser.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Collection<ChannelAssociation> getChannelAssociations() {
        ArrayList<ChannelAssociation> resultList = new ArrayList<>();
        try {
            PreparedStatement getAllUsers = Database.getPreparedStatement("getAllChannels");
            ResultSet results = getAllUsers.executeQuery();
            while (results.next()) {
                ChannelAssociation entry = new ChannelAssociation();
                entry.inGameChannelName = results.getString(1);
                entry.discordChannelId = results.getString(2);
                resultList.add(entry);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultList;
    }

    public class ChannelAssociation {
        public String inGameChannelName;
        public String discordChannelId;
    }

    public Collection<UserAssociation> getUserAssociations() {
        ArrayList<UserAssociation> resultList = new ArrayList<>();
        try {
            PreparedStatement getAllUsers = Database.getPreparedStatement("getAllUsers");
            ResultSet results = getAllUsers.executeQuery();
            while (results.next()) {
                UserAssociation entry = new UserAssociation();
                entry.inGameId = results.getString(1);
                entry.discordID = results.getString(2);
                resultList.add(entry);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultList;
    }

    public class UserAssociation {
        public String inGameId;
        public String discordID;
    }
}
