XMPP Link
=========

This bot relays messages from a simple and propietary format to an XMPP server.
The protocol is of little interest for the general population, but the bot is
being open sourced for educational purposes.

Protocol Details
================

The server listens for connections on TCP Socket port 31415. Once established,
the message stream begins immediately; there's no handshake or authentication.
The bot does not currently send messages to the server, but the format for
messages is exactly the same in both directions.

    __int16  Packet size.
    __int8   Message type; 0x64 is "Send to Channel".
    __int32  User ID.
    __int8   Size of chat room name.
    char*    Chat room name.
    __int8   Size of the message.
    char*    Message.
