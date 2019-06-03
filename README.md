# DiscordLink

## Introduction

DiscordLink is a Java-based tool to link a City of Heroes chat channel to a Discord channel.

As of writing of this README, this tool will only work with i25/SCoRE-based servers.

## Running the DiscordLink bot

### Step 1 : Creating a Discord application and bot

To create a Discord application, you should go to https://discordapp.com/developers/applications/ and select "New Application".

You will then need to add a "Discord Bot". Click on the "Bot" button on the sidebar, and select "Create Bot".

You probably want to add the bot to your server :
- Get the bot "client ID" number. It can be found on the "General Information" page. Copy this number.
- On the "Bot" page, go to the "Bot Permissions" section and check the "send messages" and "manage webhooks" options. You'll get a number in the box below (at the time of writing, it is `536872960`). Copy this number.
- Add the bot to your server. For that, you need to go to an OAuth2 url built using the  and the number generated above :
    - Create an URL that looks like `https://discordapp.com/oauth2/authorize?client_id=<ClientID>&scope=bot&permissions=<number>`, by replacing `<ClientID>` and `<number>` with the values from above
    - Go to the URL above in your browser
    - Select the Discord server you want, and click on "Authorize" to add the bot.

You will need the 'bot token' of the Discord application. It is available after creating the Bot, under the "bot" tab (there is a "click to reveal" link in the middle of the description).

### Step 2 : Create a City of Heroes default account

The default account is used to transmit messages for non mapped users. The messages will show as : `[Default Account] Discord Name : Message`

Steps :
- Create a new account on your City of Heroes server
- Create a character on this account
- Get the ID of the account (browse the `cohchat` database, `dbo.users` table, and find the `user_id` for the newly created account)

### Step 3 : Setting up the DiscordLink tool

Running the DiscordLink tool requires Java 8 or higher.

Download the latest version from [the Releases tab](https://github.com/PC-Logix/DiscordLink/releases). You can then run the tool with `java -jar DiscordLink.jar`

On first start, a series of questions will be asked to setup the bot :
- The Discord bot token (in step 1)
- The default global ID (in step 2)
- The IP of the ChatServer
- The HTTPd port used for script interfacing. The instructions below assume you chose 9001, but you can choose any other port.
- The HTTPd secret. Choose a "strong" secret, it acts as a password for all of your bot's configuration.

Once that is done, the DiscordLink tool will be running !

### Step 4 : use the HTTP configuration page to configure the tool 

You can configure the bot integration at the URL `http://localhost:9001` - adjust the URL relative to the IP of your server and the port you chose above.

From this page, you can :
- Connect City of Heroes channels to Discord channels.
- Connect City of Heroes user accounts to Discord user accounts.

In addition to using the UI to add or remove channels, you can also send HTTP GET requests to the following endpoints :
- `http://localhost:9001/?action=addChan&secret=<secret>&discordid=<DiscordChannelID>&gname=<COHChannelName>`
    - Adds a new link between the Discord channel `<DiscordChannelID>` and the City of Heroes channel `<COHChannelName>`
- `http://localhost:9001/?action=delChan&secret=<secret>&discordid=<DiscordChannelID>&gname=<COHChannelName>`
    - Removes an existing link between the Discord channel `<DiscordChannelID>` and the City of Heroes channel `<COHChannelName>`
- `http://localhost:9001/?action=addUser&secret=<secret>&discordid=<DiscordUserID>&gname=<COHUserID>`
    - Associates the  Discord account `<DiscordUserID>`with the City of Heroes account `<COHUserID>`
- `http://localhost:9001/?action=delUser&secret=<secret>&discordid=<DiscordUserID>`
    - Removes any existing association for the Discord user `<DiscordUserID>`

**Note about linked accounts** : once the accounts are linked, any message sent on Discord will show up in-game as if sent from the player's character.

### Step 5 (optional) : Add WebHook integration to your server

In your Discord Server, go to the settings of the channel you want. Go to the "Webhook" section, and create a new WebHook named "GlobalChat".

The WebHook will be auto detected. New messages will be sent as if by a Discord user, except there will be a blue `bot` tag next to the username. The avatar for users with no Discord association will be the image of the Webhook.

**Note about linked accounts** : If the Discord account and the Game accounts of the person speaking are linked, then any messages sent from the game will appear using the avatar of the Discord user. The name of the user as displayed on Discord will still be the in-game character name instad of the Discord account name.

## ChatServer Protocol Details

The server listens for connections on TCP Socket port `31415`. Once established, the message stream begins immediately ; there's no handshake or authentication.

### Packets transmitted from the ChatServer to the DiscordLink tool

Format of a normal message :
```
    __int16  Packet size.
    __int8   Message type; 0x64 is "Send to Channel".
    __int32  User ID.
    __int8   Size of chat room name.
    char*    Chat room name.
    __int8   Size of the message.
    char*    Message.
    __int8   Size of the user nickname.
    char*    Nickname
```

Format of the special "Message of the Day" message. This is sent when the Message of the Day changes.
```
    __int16  Packet size.
    __int8   Message type; 0x65 is "Message of the Day".
    __int32  User ID.
    __int8   Size of chat room name.
    char*    Chat room name.
    __int8   Size of the message.
    char*    Message.
```

### Packets transmitted from the DiscordLink tool to the ChatServer

Used to send a message to thhe given channel. The message will be sent to the user
```
    __int16  Packet size.
    __int8   Message type; 0x64 is "Send to Channel".
    __int32  User ID.
    __int8   Size of chat room name.
    char*    Chat room name.
    __int8   Size of the message.
    char*    Message.
    __int8   Size of the user nickname.
    char*    Nickname (ignored by the ChatServer)
```
**Note** : The user nickname is always sent by the server, but ignored when the server receives a mesage; it retrieves the nickname from its internal user table.
