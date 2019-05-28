# DiscordLink
Java based CoH Global to Discord relay using JDA
=========

This bot relays messages from a simple and propietary format to an XMPP server.
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
receives a mesage; it retrieves the nickname from its insternal user table.

User List
=========

The file xmpplink.user.txt contains the map from the internal User ID (int)
to the user portion of the XMPP JID. Note that if the XMPP server does not
send the full JID for users as they change nicknames, this will not work.
