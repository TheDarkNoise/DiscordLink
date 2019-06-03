package pcl.bridgebot.webserver;

import java.util.ArrayList;
import java.util.Collection;

import pcl.bridgebot.database.DatabaseHandler;
import pcl.bridgebot.discordserverlink.DiscordServerLink;

public class HTTPDatabaseAdapter {
    private final DatabaseHandler database;
    private final DiscordServerLink serverLink;

    public HTTPDatabaseAdapter(DatabaseHandler database, DiscordServerLink serverLink) {
        this.database = database;
        this.serverLink = serverLink;
    }

	public Collection<DatabaseAdapterUserResult> getAllUsers() {
        ArrayList<DatabaseAdapterUserResult> resultList = new ArrayList<>();
        for(DatabaseHandler.UserAssociation assocation :  database.getUserAssociations()) {
            DatabaseAdapterUserResult entry = new DatabaseAdapterUserResult();
            entry.inGameId = assocation.inGameId;
            entry.discordID = assocation.discordID;
            entry.discordName = serverLink.getUsernameFromId(assocation.discordID);
            resultList.add(entry);
        }
		return resultList;
    }
    
    public class DatabaseAdapterUserResult {
        public String discordID;
        public String discordName;
        public String inGameId;
    }

	public Collection<DatabaseAdapterChannelResult> getAllChannels() {
        ArrayList<DatabaseAdapterChannelResult> resultList = new ArrayList<>();
        for(DatabaseHandler.ChannelAssociation assocation :  database.getChannelAssociations()) {
            DatabaseAdapterChannelResult entry = new DatabaseAdapterChannelResult();
            entry.inGameChannelName = assocation.inGameChannelName;
            entry.discordID = assocation.discordChannelId;
            entry.discordServerName = serverLink.getServerNameFromChannelId(assocation.discordChannelId);
            entry.discordChannelName = serverLink.getChannelNameFromId(assocation.discordChannelId);
            resultList.add(entry);
        }
		return resultList;
    }
    
    public class DatabaseAdapterChannelResult {
        public String discordID;
        public String discordServerName;
        public String discordChannelName;
        public String inGameChannelName;
    }
}