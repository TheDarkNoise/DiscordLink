# DiscordLink
Java based CoH Global to Discord relay using JDA (Only for i25/SCoRE based servers currently)
=========

This bot relays messages from a simple and propietary format to a Discord Guild.
The protocol is of little interest for the general population, but the bot is
being open sourced for educational purposes.

Protocol Details
================

The server listens for connections on TCP Socket port 31415. Once established,
the message stream begins immediately; there's no handshake or authentication.

    __int16  Packet size.
    __int8   Message type; 0x64 is "Send to Channel".
    __int32  User ID.
    __int8   Size of chat room name.
    char*    Chat room name.
    __int8   Size of the message.
    char*    Message.
    __int8   Size of the user nickname.
    char*    Nickname

The user nickname is always sent by the server, but ignored when the server
receives a mesage; it retrieves the nickname from its internal user table.

Setup
=========

On Initial run of the bridge you will be asked to provide the Discord Bot token,
you can get this at https://discordapp.com/developers/applications/ and setting
up a new Application.

You will also need a default global id for non mapped users to send from,
create an account, create a character, and login to create the entry in the
cohchat db, browse dbo.users, and find the 'user_id' for your user

The httpd port you select will be how you administer your server, you can also send
get requests to the endpoints directly to automate adding users.

Finally you will be asked to provide an httpsecret, this is like the password to the panel,
it is required for *all* actions on the panel this is to stop someone from finding your server
and adding/deleting data.
