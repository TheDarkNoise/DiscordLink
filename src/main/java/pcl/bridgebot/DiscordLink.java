package pcl.bridgebot;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Scanner;

import javax.security.auth.login.LoginException;

import emoji4j.EmojiUtils;
import pcl.bridgebot.chatserverlink.ChatServerLink;
import pcl.bridgebot.chatserverlink.IPackedMessageData;
import pcl.bridgebot.chatserverlink.ServerAlreadyRunningException;
import pcl.bridgebot.database.BaseSettings;
import pcl.bridgebot.database.DatabaseHandler;
import pcl.bridgebot.discordserverlink.DiscordServerLink;
import pcl.bridgebot.discordserverlink.IDiscordMessageData;
import pcl.bridgebot.webserver.HTTPd;

public class DiscordLink {

	private final ChatServerLink link;
	private final DiscordServerLink discordServerLink;
	private final DatabaseHandler databaseHandler;

	private final String defaultGID;

	/**
	 * This is the method where the program starts.
	 * 
	 * @throws Exception
	 */
	public static void main(String[] args) {

		try {
			new DiscordLink();
		} catch (LoginException | InterruptedException e) {
			// If anything goes wrong with the Discord authentification, this is the
			// exception that
			// will represent it
			e.printStackTrace();
			System.out.println("DiscordLink initialization Failure!");
			return;
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println("Database Failure!");
			return;
		} catch (ClassNotFoundException e) {
			System.out.println(e.getMessage());
			System.out.println("JDBC Failure!");
			return;
		}
	}

	public DiscordLink() throws LoginException, InterruptedException, SQLException, ClassNotFoundException {
		// Initialize JDBC
		Class.forName("org.sqlite.JDBC");

		// Initialize the database
		if (!DatabaseHandler.initialize()) {
			System.out.println("Database Failure!");
		}

		databaseHandler = new DatabaseHandler();

		// Get the settings
		BaseSettings settings = null;
		try {
			Optional<BaseSettings> maybeSettings = databaseHandler.getSettings();
			if (maybeSettings.isPresent()) {
				settings = maybeSettings.get();

			} else {
				// If the settings aren't set, request them from the console
				settings = requestSettingsToUser();
				databaseHandler.setSettings(settings);
			}
		} catch (Exception e1) {
			e1.printStackTrace();
			System.out.println("Error while loading or saving settings !");
		}
		defaultGID = settings.getDefaultGID();

		// Initialize the ChatServer link
		System.out.println("Initializing ChatServer connection towards " + settings.getChatserverIP() + ":"
				+ settings.getChatserverport());
		link = new ChatServerLink(settings.getChatserverIP(), settings.getChatserverport());

		// Initialize the DiscordServer link
		discordServerLink = new DiscordServerLink(settings.getDiscordToken(),
				() -> databaseHandler.getCustomSettings().getDefaultWebhookName(),
				() -> databaseHandler.getCustomSettings().getFormatterMode(), packet -> {
					handleDiscordMessage(packet);
				});

		// Initialize the HTTPd server
		try {
			HTTPd httpServer = new HTTPd();
			httpServer.setup(settings.getHttpdPort());
			httpServer.registerPages(settings.getHttpdSecret(), databaseHandler, discordServerLink);
			httpServer.start(settings.getHttpdPort());
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		// Start the listening loop for the ChatServer on another thread
		Runnable inGameLinkThreadRunner = new Runnable() {
			@Override
			public void run() {
				try {
					link.startLoop(t -> {
						handleInGameMessage(t);
					});
				} catch (ServerAlreadyRunningException e) {
					// NO OP, should never happen
				}
			}
		};
		Thread inGameLinkThread = new Thread(inGameLinkThreadRunner);
		inGameLinkThread.start();
	}

	public static BaseSettings requestSettingsToUser() {
		// We've got a fresh database, we need to ask some questions.
		System.out.println("Enter your Discord Bot token: ");
		Scanner scanner = new Scanner(System.in);
		String discordToken = scanner.nextLine();

		System.out.println("Enter the default *ID* for global messages to show in game.");
		System.out.println("(This will be the 'user_id' column in the 'cohchat' DB: ");
		String defaultGID = scanner.nextLine();

		System.out.println("IPAddress for your chatserver (127.0.0.1?): ");
		String chatserverIP = scanner.nextLine();

		System.out.println("HTTPd port for external script interfacing: ");
		int httpdPort = Integer.parseInt(scanner.nextLine());

		System.out.println("HTTPd secret, basically the password sent with all get requests: ");
		String httpdSecret = scanner.nextLine();

		System.out.println(
				"If you need to modify these settings you can open 'discordlink.sqlite3' in an SQLite editor.");
		scanner.close();

		return new BaseSettings(chatserverIP, 31415, defaultGID, discordToken, httpdPort, httpdSecret);
	}

	private void handleInGameMessage(IPackedMessageData inMsg) {
		try {

			Optional<String> discordUserId = databaseHandler.getDiscordId(inMsg.getUserId());

			for (String discordChannelId : databaseHandler.getDiscordChannelListFromGame(inMsg.getChatroom())) {

				try {
					discordServerLink.sendMessageToChannel(discordUserId, inMsg.getUserNickname(), inMsg.getMessage(),
							discordChannelId);
				} catch (Exception e1) {
					System.out.println(e1);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void handleDiscordMessage(IDiscordMessageData discordMessage) {
		Iterable<String> channelList = databaseHandler
				.getGameChannelListFromDiscord(discordMessage.getDiscordChannelId());

		Optional<Integer> maybeCharacterId = databaseHandler.getCharacterId(discordMessage.getUserId());

		boolean isCharacterLinked = maybeCharacterId.isPresent();

		// Prepare the message for the Game server
		String inGameMsg = discordMessage.getMessage();
		inGameMsg = EmojiUtils.shortCodify(inGameMsg);

		// Get the character ID to send towards. If needed, prepend the Discord username
		// to the message.
		Integer characterId = maybeCharacterId.orElse(Integer.valueOf(defaultGID));
		if (!isCharacterLinked) {
			inGameMsg = discordMessage.getUserName() + ": " + inGameMsg;
		}

		// This will store the channels to echo towards. Use a HashSet to prevent
		// echoing several times to the same channel.
		HashSet<String> disscordChannelsToEchoTo = new HashSet<>();

		// Send the messages to all linked in-game channels.
		for (String channelId : channelList) {

			// Send the message to the in-game channel
			link.sendMessage(channelId, characterId, "DiscordLink", inGameMsg);

			// Retrieve the ID of other Discord channels to echo towards
			disscordChannelsToEchoTo.addAll(databaseHandler.getDiscordChannelListFromGame(channelId));
		}

		// Echo the message to the other Discord channels
		for (String echoDiscordChannel : disscordChannelsToEchoTo) {
			// Do not send the message to the current channel.
			if (discordMessage.getDiscordChannelId().equals(echoDiscordChannel))
				continue;
			discordServerLink.sendMessageToChannel(Optional.of(discordMessage.getUserId()),
					discordMessage.getUserName(), discordMessage.getMessage(), echoDiscordChannel);
		}
	}
}