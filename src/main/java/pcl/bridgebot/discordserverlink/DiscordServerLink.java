package pcl.bridgebot.discordserverlink;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import javax.security.auth.login.LoginException;

import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.Webhook;
import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import pcl.bridgebot.DiscordLink;

public class DiscordServerLink {

    private static Supplier<String> defaultWebhookNameGetter = null;

    private static Supplier<String> formatterModeGetter = null;

    public static JDA jda;

    public DiscordServerLink(String discordToken, Supplier<String> defaultWebhookNameGetter,
            Supplier<String> formatterModeGetter, Consumer<IDiscordMessageData> messageHandler)
            throws LoginException, InterruptedException {
        this.defaultWebhookNameGetter = defaultWebhookNameGetter;
        this.formatterModeGetter = formatterModeGetter;
        jda = JDABuilder.createDefault(discordToken)
                .addEventListeners(new MessageListenerAdapter(this, messageHandler)) // The class that will handle events
                .setChunkingFilter(ChunkingFilter.ALL)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                .build();
        jda.awaitReady(); // Blocking guarantees that JDA will be completely loaded.
        System.out.println("Finished Building JDA!");
    }

    public void ensureJDAFromEvent(JDA jda) {
        this.jda = jda;
    }

    public static void sendMessageToChannel(Optional<String> userId, String userNickname, String message,
                                            String discordChannelId) {

        // Get the Discord TextChannel instance by the ID
        TextChannel channel = jda.getTextChannelById(discordChannelId);
        try {
            // Try to send the message over a Webhook if available
            List<Webhook> webhook = channel.retrieveWebhooks().complete();
            for (Webhook hook : webhook) {
                // Check if the Webhook name matches the required name.
                if (!hook.getName().equalsIgnoreCase(defaultWebhookNameGetter.get()))
                    continue;
                sendWebhookMessage(userId, userNickname, message, channel, hook.getUrl());
                return;
            }

        } catch (Exception e) {
            // NO OP : ignore error if the WebHook retrieval failed
            // The reason is that this error can be due the the WebHook management
            // permissions not being given to the bot
        }

        // If we get here, we didn't find any WebHook (or the WebHook list retrieval
        // failed for some reason)
        // Send the message as "raw" message
        sendRawMessage(userNickname, message, channel);
    }

    public static void sendRawMessage(String userNickname, String message, TextChannel channel) {
        // Format the message to include @ mentions to the users from the channel
        List<Member> members = channel.getMembers();
        for (Member m : members) {
            if (message.toLowerCase().contains("@" + m.getEffectiveName().toLowerCase())) {
                message = message.replace("@" + m.getEffectiveName(), m.getAsMention());
            }
            if (message.toLowerCase().contains("@" + m.getUser().getName().toLowerCase())) {
                message = message.replace("@" + m.getUser().getName(), m.getAsMention());
            }
        }
        // Format the message to disable @ everyone and @ here (by adding a zero-width
        // character in the middle of them)
        message = message.replace("@everyone", "@" + "\u00a0" + "everyone");
        message = message.replace("@here", "@" + "\u00a0" + "here");

        // Send the message to the channel
        channel.sendMessage(userNickname + ": " + message).queue();
    }

    public static void sendWebhookMessage(Optional<String> userId, String inGameUsername, String message, TextChannel channel,
                                          String hook) {
        String formattedMessage = message;

        // Format the message to include @ mentions to the users from the channel
        List<Member> members = channel.getMembers();
        for (Member m : members) {
            if (formattedMessage.toLowerCase().contains("@" + m.getEffectiveName().toLowerCase())) {
                formattedMessage = formattedMessage.replace("@" + m.getEffectiveName(), m.getAsMention());
            }
            if (formattedMessage.toLowerCase().contains("@" + m.getUser().getName().toLowerCase())) {
                formattedMessage = formattedMessage.replace("@" + m.getUser().getName(), m.getAsMention());
            }
        }

        // Format the message to disable @ everyone and @ here (by adding a zero-width
        // character in the middle of them)
        formattedMessage = formattedMessage.replace("@everyone", "@" + "\u00a0" + "everyone");
        formattedMessage = formattedMessage.replace("@here", "@" + "\u00a0" + "here");
        WebhookClient client = null;
        // Prepare to send the message over the webhook
        try {
            client = WebhookClient.withUrl(hook.replace("discord.com", "discordapp.com").replace("v6/", ""));
        } catch (Exception e) {
            e.printStackTrace();
        }
        Optional<String> discordUsername = Optional.empty();
        WebhookMessageBuilder builder = new WebhookMessageBuilder();

        User foundUser;
        if (userId.isPresent() && (foundUser = jda.getUserById(userId.get())) != null) {
            // Use the user avatar for the WebHook message.
            builder.setAvatarUrl(foundUser.getAvatarUrl());

            // Get the Channel User name, or global user name if the channel name is
            // unavailable
            Optional<String> maybeMatchingMemberName = members.stream()
                    .filter(u -> u.getUser().getId().equals(userId.get())) // Find the user
                    .map(u -> u.getEffectiveName()) // Get the user effective name
                    .findFirst();
            discordUsername = Optional.of(maybeMatchingMemberName.orElse(foundUser.getName()));
        } else {
            // Otherwise, fall back on the default hook avatar for this WebHook.
            //builder.setAvatarUrl(builder.);
        }

        // Set the username for the WebHook message, sung the given formatter
        builder.setUsername(getFormattedUsername(discordUsername, inGameUsername));
        // Set the contents for the WebHook message
        builder.setContent(message.replaceFirst(Pattern.quote("<" + inGameUsername + ">"), ""));

        // Send the message to the WebHook
        //WebhookMessage webhookMessage = messageBuilder.build();
        client.send(builder.build());
    }

    public boolean isSelfUser(User author) {
        if (author == null)
            return false;
        return author.equals(jda.getSelfUser());
    }

    public String getUsernameFromId(String userId) {
        try {
            return jda.getUserById(userId).getName();
        }
        catch(Exception e) {
            return "Could not retrieve user name (user ID invalid or possibly deleted)";
        }
    }

    public String getServerNameFromChannelId(String channelId) {
        try {
            return jda.getTextChannelById(channelId).getGuild().getName();
        }
        catch(Exception e) {
            return "Could not retrieve server name (channel ID invalid or possibly deleted)";
        }
    }

    public String getChannelNameFromId(String channelId) {
        try {
            return jda.getTextChannelById(channelId).getName();
        }
        catch(Exception e) {
            return "Could not retrieve channel name (channel ID invalid or possibly deleted)";
        }
    }

    private static String getFormattedUsername(Optional<String> discordUsername, String inGameUsername) {
        String formatterMode = "InGame";
        if (formatterModeGetter != null) {
            formatterMode = formatterModeGetter.get();
        }

        switch (formatterMode) {
        case "Mixed":
            if (discordUsername.isPresent() && !discordUsername.get().equals(inGameUsername)) {
                return discordUsername.get() + " (" + inGameUsername + ")";
            }
            return inGameUsername;
        case "Discord":
            return discordUsername.orElse(inGameUsername);
        default:
        case "InGame":
            return inGameUsername;
        }
    }
}