/**
 * 
 */
package pcl.HeroOne;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.webhook.WebhookClient;
import net.dv8tion.jda.webhook.WebhookClientBuilder;
import net.dv8tion.jda.webhook.WebhookMessage;
import net.dv8tion.jda.webhook.WebhookMessageBuilder;
import pcl.HeroOne.ChatServerLink.PackedMessageData;
import pcl.HeroOne.util.Database;
import pcl.HeroOne.util.httpd;

import javax.security.auth.login.LoginException;

import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class HeroOne extends ListenerAdapter
{
	static ChatServerLink link = null;
	public static Integer httpdPort = null;
	static JDA jda; 
	public static httpd httpServer = new httpd();
	
	private static boolean initDatabase() throws SQLException {
		Database.init();
		Database.addStatement("CREATE TABLE IF NOT EXISTS Channels(globalName PRIMARY KEY, discordID)");
		Database.addStatement("CREATE TABLE IF NOT EXISTS UserMap(globalID PRIMARY KEY, discordID)");
		Database.addStatement("CREATE TABLE IF NOT EXISTS Config(key PRIMARY KEY, data)");
		Database.addStatement("CREATE TABLE IF NOT EXISTS JsonData (mykey VARCHAR(255) PRIMARY KEY NOT NULL, store TEXT DEFAULT NULL);");

		//Channels
		Database.addPreparedStatement("addChannel", "REPLACE INTO Channels (globalName, discordID) VALUES (?,?);");
		Database.addPreparedStatement("removeChannel","DELETE FROM Channels WHERE globalName = ?;");
		Database.addPreparedStatement("getChannelByGlobal","SELECT * FROM Channels WHERE globalName = ?;");
		Database.addPreparedStatement("getChannelByDiscordID","SELECT * FROM Channels WHERE discordID = ?;");
		
		//UserMap
		Database.addPreparedStatement("getUserByGlobal","SELECT discordID FROM UserMap WHERE globalID = ?;");
		Database.addPreparedStatement("getUserByDiscordID","SELECT globalID FROM UserMap WHERE discordID = ?;");
		
		//JSONStorage
		Database.addPreparedStatement("storeJSON", "INSERT OR REPLACE INTO JsonData (mykey, store) VALUES (?, ?);");
		Database.addPreparedStatement("retreiveJSON", "SELECT store FROM JsonData WHERE mykey = ?");

		//Default config stuff
		Database.addPreparedStatement("getSettings","SELECT data FROM Config WHERE key = ? LIMIT 1;");
		return true;
	}

	/**
	 * This is the method where the program starts.
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception
	{
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			System.out.println(e.getMessage());
			return;
		}
		try {
			if (!initDatabase()) {
				System.out.println("Database Failure!");
				return;
			}
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		PreparedStatement getSettings = Database.getPreparedStatement("getSettings");
		String chatserver = null;
		Integer chatserverport = null;
		getSettings.setString(1, "chatserver");
		ResultSet res1 = getSettings.executeQuery();
		if (res1.next()) {
			chatserver = res1.getString(1);
		}
		
		getSettings.setString(1, "chatserverport");
		ResultSet res2 = getSettings.executeQuery();
		if (res2.next()) {
			chatserverport = res2.getInt(1);
		}
		
		getSettings.setString(1, "httpdport");
		ResultSet res3 = getSettings.executeQuery();
		if (res3.next()) {
			httpdPort = res3.getInt(1);
		}
		
		 link = new ChatServerLink(chatserver, chatserverport);
		
		//We construct a builder for a BOT account. If we wanted to use a CLIENT account
		// we would use AccountType.CLIENT
		try
		{
			getSettings.setString(1, "discordToken");
			ResultSet results = getSettings.executeQuery();
			if (results.next()) {
				jda = new JDABuilder(results.getString(1))         // The token of the account that is logging in.
						.addEventListener(new HeroOne())  // An instance of a class that will handle events.
						.build();
				jda.awaitReady(); // Blocking guarantees that JDA will be completely loaded.
				System.out.println("Finished Building JDA!");
			} else {
				System.out.println("Set Discord Token!");
			}

		}
		catch (LoginException e)
		{
			//If anything goes wrong in terms of authentication, this is the exception that will represent it
			e.printStackTrace();
		}
		catch (InterruptedException e)
		{
			//Due to the fact that awaitReady is a blocking method, one which waits until JDA is fully loaded,
			// the waiting can be interrupted. This is the exception that would fire in that situation.
			//As a note: in this extremely simplified example this will never occur. In fact, this will never occur unless
			// you use awaitReady in a thread that has the possibility of being interrupted (async thread usage and interrupts)
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		senderClient();
	}

	/**
	 * NOTE THE @Override!
	 * This method is actually overriding a method in the ListenerAdapter class! We place an @Override annotation
	 *  right before any method that is overriding another to guarantee to ourselves that it is actually overriding
	 *  a method from a super class properly. You should do this every time you override a method!
	 *
	 * As stated above, this method is overriding a hook method in the
	 * {@link net.dv8tion.jda.core.hooks.ListenerAdapter ListenerAdapter} class. It has convience methods for all JDA events!
	 * Consider looking through the events it offers if you plan to use the ListenerAdapter.
	 *
	 * In this example, when a message is received it is printed to the console.
	 *
	 * @param event
	 *          An event containing information about a {@link net.dv8tion.jda.core.entities.Message Message} that was
	 *          sent in a channel.
	 */
	@Override
	public void onMessageReceived(MessageReceivedEvent event)
	{
		//These are provided with every event in JDA
		jda = event.getJDA();                       //JDA, the core of the api.
		long responseNumber = event.getResponseNumber();//The amount of discord events that JDA has received since the last reconnect.

		//Event specific information
		User author = event.getAuthor();                //The user that sent the message
		Message message = event.getMessage();           //The message that was received.
		MessageChannel channel = event.getChannel();    //This is the MessageChannel that the message was sent to.
		//  This could be a TextChannel, PrivateChannel, or Group!

		String msg = message.getContentDisplay();              //This returns a human readable version of the Message. Similar to
		// what you would see in the client.

		boolean bot = author.isBot();                    //This boolean is useful to determine if the User that
		// sent the Message is a BOT or not!

		if (event.isFromType(ChannelType.TEXT))         //If this message was sent to a Guild TextChannel
		{
			//Because we now know that this message was sent in a Guild, we can do guild specific things
			// Note, if you don't check the ChannelType before using these methods, they might return null due
			// the message possibly not being from a Guild!

			Guild guild = event.getGuild();             //The Guild that this message was sent in. (note, in the API, Guilds are Servers)
			TextChannel textChannel = event.getTextChannel(); //The TextChannel that this message was sent to.
			Member member = event.getMember();          //This Member that sent the message. Contains Guild specific information about the User!

			String name;
			if (message.isWebhookMessage())
			{
				name = author.getName();
				return; //If this is a Webhook message, then there is no Member associated also dump here we don't wanna see it in game.
			}                                           // with the User, thus we default to the author for name.
			else
			{
				name = member.getEffectiveName();       //This will either use the Member's nickname if they have one,
			}                                           // otherwise it will default to their username. (User#getName())

			System.out.printf("(%s)[%s]<%s>: %s\n", guild.getName(), textChannel.getName(), name, msg);
			if (author.equals(jda.getSelfUser()))
				return;
			PreparedStatement getChannelByDiscordID;
			PreparedStatement getUserByDiscordID;
			try {
				getChannelByDiscordID = Database.getPreparedStatement("getChannelByDiscordID");
				getChannelByDiscordID.setString(1, event.getTextChannel().getId());
				ResultSet results = getChannelByDiscordID.executeQuery();
				
				getUserByDiscordID = Database.getPreparedStatement("getUserByDiscordID");
				getUserByDiscordID.setString(1, event.getAuthor().getId());
				ResultSet results2 = getUserByDiscordID.executeQuery();
				if (results.next()) {
					if (results2.next()) {
						link.sendMessage(results.getString(1), Integer.valueOf(results2.getString(1)), "TestUser", msg);
					}
					else {
						link.sendMessage(results.getString(1), 128, "TestUser",name + ": " + msg);
					}
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	//Game to Discord.
	private static void senderClient() {

		Runnable test = new Runnable(){
			@Override
			public void run() {
				link.startLoop(new Consumer<ChatServerLink.PackedMessageData>() {
					@Override
					public void accept(PackedMessageData t) {
						//
						PreparedStatement getChannelByGlobal;
						try {
							getChannelByGlobal = Database.getPreparedStatement("getChannelByGlobal");
							getChannelByGlobal.setString(1, t.chatroom);
							ResultSet results = getChannelByGlobal.executeQuery();
							if (results.next()) {
								System.out.println(results.getInt(2));
								System.out.println("[" + t.userNickname + "] " + t.message);
								TextChannel channel = HeroOne.jda.getTextChannelById(results.getString(2));
								//channel.sendMessage(t.userNickname + ": " + t.message).queue();
								try
								{
									List<Webhook> webhook = channel.getWebhooks().complete(); // some webhook instance
									if (webhook.size() == 0) {
										throw new RuntimeException();
									}
									for (Webhook hook : webhook) {
										if (hook.getName().equalsIgnoreCase("GlobalChat")) {
											String nick = t.userNickname;
											WebhookClientBuilder builder = hook.newClient(); //Get the first webhook.. I can't think of a better way to do this ATM.
											WebhookClient client = builder.build();
											WebhookMessageBuilder builder1 = new WebhookMessageBuilder();
											builder1.setContent(t.message.replaceFirst(Pattern.quote("<"+t.userNickname+">"), ""));
											//MessageEmbed firstEmbed = new EmbedBuilder().setColor(Color.RED).setDescription("This is one embed").build();
											//MessageEmbed secondEmbed = new EmbedBuilder().setColor(Color.GREEN).setDescription("This is another embed").build();
											builder1.setUsername(nick);
											String avatar = "";
											avatar = hook.getDefaultUser().getAvatarUrl();
											builder1.setAvatarUrl(avatar);
											WebhookMessage message1 = builder1.build();
											client.send(message1);
											client.close();
											return;
										}
									}
									channel.sendMessage(t.userNickname + ": " + t.message).queue();
								} catch (Exception e1) {
									channel.sendMessage(t.userNickname + ": " + t.message).queue();
								}
							}	
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				});
			}
		};
		Thread thread = new Thread(test);
		thread.start();
	}

}