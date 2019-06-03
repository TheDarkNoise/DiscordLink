package pcl.bridgebot.discordserverlink;

import java.util.function.Consumer;

import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import pcl.bridgebot.discordserverlink.impl.DiscordMessageData;

public class MessageListenerAdapter extends ListenerAdapter {

    private DiscordServerLink serverLink;
    private Consumer<IDiscordMessageData> discordMessageHandler;

    public MessageListenerAdapter(DiscordServerLink serverLink, Consumer<IDiscordMessageData> discordMessageHandler) {
        this.serverLink = serverLink;
        this.discordMessageHandler = discordMessageHandler;
    }

    /**
     * @param event An event containing information about a
     *              {@link net.dv8tion.jda.core.entities.Message Message} that was
     *              sent in a channel.
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // These are provided with every event in JDA
        serverLink.ensureJDAFromEvent(event.getJDA()); // JDA, the core of the api.

        // Event specific information
        User author = event.getAuthor(); // The user that sent the message

        if (serverLink.isSelfUser(author)) {
            // If this message was sent from our bot, ignore it.
            return;
        }

        Message discordMessage = event.getMessage(); // The message that was received.
        // This returns a human readable version of the Message. Similar to what you
        // would see in the client.
        String textMessage = discordMessage.getContentDisplay();

        if (!event.isFromType(ChannelType.TEXT)) {
            // Only handle messages sent using a Guild TextChannel
            return;
        }

        // Because we now know that this message was sent in a Guild, we can do guild
        // specific things
        // Note, if you don't check the ChannelType before using these methods, they
        // might return null due
        // the message possibly not being from a Guild!

        event.getGuild();
        event.getTextChannel();
        Member member = event.getMember(); // This Member that sent the message. Contains Guild specific information
        // about the User!

        if (discordMessage.isWebhookMessage()) {
            // If this is a Webhook message, then there is no Member associated.
            // Stop processing here, we don't wanna see it in game.
            return;
        }

        // with the User, thus we default to the author for name.
        String userName = member.getEffectiveName(); // This will either use the Member's nickname if they have one,
        // otherwise it will default to their username. (User#getName())

        IDiscordMessageData craftedMessageData = new DiscordMessageData(event.getTextChannel().getId(),
                event.getAuthor().getId(), userName, textMessage);
        discordMessageHandler.accept(craftedMessageData);
    }
}