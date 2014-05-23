#pragma comment(lib, "gloox-1.0.lib")
#pragma comment(lib, "ws2_32.lib") 

#define GLOOX_IMPORTS

#include "gloox/src/client.h"
#include "gloox/src/connectionlistener.h"
#include "gloox/src/mucroomhandler.h"
#include "gloox/src/mucroom.h"
#include "gloox/src/disco.h"
#include "gloox/src/presence.h"
#include "gloox/src/message.h"
#include "gloox/src/dataform.h"
#include "gloox/src/gloox.h"
#include "gloox/src/lastactivity.h"

#include <stdio.h>
#include <locale.h>
#include <string>
#include <cstdio>
#include <windows.h>
#include <map>
#include <fstream>
#include <algorithm>

#define XMPPLINK_PORT 31415

#define XMPPLINK_STATE_NONE 0
#define XMPPLINK_STATE_READY 2

#define XMPPLINK_COMMAND_CHANSEND 100

// TODO: This is horrible and I should feel bad about it.
// Use minIni to load these basic settings instead.
#define XMPPLINK_NICKNAME "ChatBot"
#define XMPPLINK_FULL_JID "chatbot@xmpp.server.com/Chat";
#define XMPPLINK_XMPPROOM "chatroom@conference.xmpp.server.com/ChatBot"
#define XMPPLINK_PASSWORD "botpassword"
#define XMPPLINK_RESOURCE "ChatBot"
#define XMPPLINK_CHATROOM "Chat Room Name"
#define XMPPLINK_SERVERIP "127.0.0.1"
#define XMPPLINK_USERFILE "xmpplink.users.txt"

class XmppLink
{
public:
	XmppLink() {
		state = XMPPLINK_STATE_NONE;
	}

	bool Connect()
	{
		struct sockaddr_in addr;

		sockfd = socket(AF_INET, SOCK_STREAM, 0);
		if (sockfd < 0)
			return false;

		memset(&addr, 0, sizeof(struct sockaddr_in));
		addr.sin_family = AF_INET;
		addr.sin_addr.s_addr = inet_addr(XMPPLINK_SERVERIP);
		addr.sin_port = htons(XMPPLINK_PORT);

		if (connect(sockfd, (struct sockaddr *)&addr, sizeof(addr)) < 0)
		{
			printf("XMPP Link Connection Error: %d\n", WSAGetLastError());
			return false;
		}

		state = XMPPLINK_STATE_READY;
		return true;
	}

	std::string Ping(std::string nick)
	{
		std::string jid;
		jid = nick_jid[nick];
		user_id = jid_auth[jid];
		jid = "[XMPP Link] Received request from " + nick + " <" + jid + ">: Messages will ";

		if (!user_id)
			jid.append("NOT ");

		jid.append("show up in ");
		jid.append(XMPPLINK_CHATROOM);

		if (!user_id)
			jid.append(" (User ID missing)");

		return jid;
	}

	void Send(std::string nick, std::string channel, std::string msg)
	{
		int s;
		std::string jid;
		if (state != XMPPLINK_STATE_READY)
			return;

		jid = nick_jid[nick];
		user_id = jid_auth[jid];

		if (!user_id)
		{
			printf("%s not in the users file!\n", nick.c_str());
			return;
		}

		buffer[2] = XMPPLINK_COMMAND_CHANSEND;

		// Using .size as a position pointer for the buffer.
		// We'll be done with it before any call to recv can touch the buffer.
		size = 3;
		memcpy(&buffer[size], &user_id, 4);
		size += 4;

		s = channel.length();
		if (s > 255) s = 255;	// Truncate if larger, though should never happen.

		memcpy(&buffer[size], &s, 1);
		size += 1;
		memcpy(&buffer[size], channel.c_str(), s);
		size += s;

		s = msg.length();
		if (s > 255) s = 255;	// Truncate if larger, TODO: split in multiple messages.

		memcpy(&buffer[size], &s, 1);
		size += 1;
		memcpy(&buffer[size], msg.c_str(), s);
		size += s;

		// Don't send the user name, server will ignore it anyway.

		// Packet size in the first 2 bytes.
		memcpy(&buffer[0], &size, 2);

		send(sockfd, buffer, size, 0);
	}

	bool Recv()
	{
		int s;
		FD_ZERO(&fd);
		FD_SET(sockfd, &fd);
		timeout.tv_sec = 0;
		timeout.tv_usec = 0;
		s = select(0, &fd, NULL, NULL, &timeout);
		if (s > 0)
		{
			size = recv(sockfd, buffer, 1000, 0);
			if (size < 1)
			{
				// select() said we had data to read but nothing came through, or an error
				// happened. Best to just drop the socket and retry the connection from scratch.
				closesocket(sockfd);
				state = XMPPLINK_STATE_NONE;
				return false;
			}
			else
			{
				// WE GOT DATA! WOOHOO!
				int packetSize = 0;
				memcpy(&packetSize, &buffer[0], 2);

				if (packetSize != size)	// Received data didn't match packet size. Drop the packet.
					return false;

				// buffer[2] is the command, not going to bother putting it somewhere else.
				if (buffer[2] == XMPPLINK_COMMAND_CHANSEND)
				{
					int len = 0;
					int idx = 3;

					// Next in buffer is the User ID of the sender of the message.
					memcpy(&user_id, &buffer[idx], 4);
					idx += 4;

					// And next, the length of the chat channel.
					memcpy(&len, &buffer[idx], 1);
					idx += 1;
					dest.assign(&buffer[idx], len);
					idx += len;

					// For now we're only relaying messages from/to a single chat room.
					if (dest.compare(XMPPLINK_CHATROOM))
						return false;

					// Message contents.
					memcpy(&len, &buffer[idx], 1);
					idx += 1;
					message.assign(&buffer[idx], len);
					idx += len;

					// User nick name.
					memcpy(&len, &buffer[idx], 1);
					idx += 1;
					user_name.assign(&buffer[idx], len);
					idx += len;
				}

				return true;
			}
		}
		else if (s == -1)
		{
			// Something is wrong with the socket, drop it and re-initialize the listener.
			closesocket(sockfd);
			state = XMPPLINK_STATE_NONE;
			return false;
		}

		return false;
	}

	bool Tick()
	{
		if (state == XMPPLINK_STATE_NONE)
		{
			printf("%s\n", Connect() ? "Connected to XMPP Link" : "Error connecting to XMPP Link");
			return false;
		}
		else
		{
			return Recv();
		}
	}

	// TODO: load this from SQL instead of a file.
	// SQL example probably not useful for Github version though, use a #define to keep both versions.
	void LoadMap()
	{
		std::ifstream userlist;
		std::string name;
		int id = 0;
		printf("Loading list of users...\n");
		userlist.open(XMPPLINK_USERFILE);
		while (userlist >> id >> name)
		{
			std::transform(name.begin(), name.end(), name.begin(), tolower);
			jid_auth.insert(std::pair<std::string, int>(name, id));
		}


		userlist.close();
		printf("Done.\n");
	}

	void NickMap(std::string nick, std::string jid)
	{
		std::transform(jid.begin(), jid.end(), jid.begin(), tolower);
		nick_jid.insert(std::pair<std::string, std::string>(nick, jid));
	}

	int user_id;
	std::string dest;
	std::string message;
	std::string user_name;

private:
	SOCKET sockfd;
	fd_set fd;
	int state;
	char buffer[1000];
	int size;
	int reconnectTime;
	struct timeval timeout;
	std::map<std::string, int> jid_auth;
	std::map<std::string, std::string> nick_jid;
};

class ParagonBot : public gloox::ConnectionListener, gloox::MUCRoomHandler
{
public:
	ParagonBot() {}

	virtual ~ParagonBot() {}

	void start()
	{
		std::string fulljid = XMPPLINK_FULL_JID;
		gloox::JID jid(fulljid);
		j = new gloox::Client(jid, XMPPLINK_PASSWORD);
		j->registerConnectionListener(this);
		j->setPresence(gloox::Presence::Available, -1);
		j->disco()->setVersion("XMPP Link", "0.10", "Windows");
		j->disco()->setIdentity("client", "bot", "XMPP Link");
		j->setCompression(false);
		gloox::StringList ca;
		ca.push_back("/path/to/cacert.crt");
		j->setCACerts(ca);

		gloox::JID nick(XMPPLINK_XMPPROOM);
		m_room = new gloox::MUCRoom(j, nick, this, 0);

		xmppLink.LoadMap();

		if (j->connect(false))
		{
			gloox::ConnectionError ce = gloox::ConnNoError;
			while (ce == gloox::ConnNoError)
			{
				// Main loop! About time!
				ce = j->recv(10000);	// 100ms timeout
				if (xmppLink.Tick())
				{
					// Got a message from the XMPP Link, relay it.
					m_room->send(xmppLink.user_name + ": " + xmppLink.message);
				}
			}
		}

		delete m_room;
		delete j;
	}

	virtual void onConnect()
	{
		m_room->join();
		m_room->getRoomInfo();
		m_room->getRoomItems();
	}

	virtual void onDisconnect(gloox::ConnectionError e)
	{
		// Bot was disconnected, probably auth failure. Should try to reconnect.
		printf("Disconnected from XMPP, retrying...\n");
	}

	virtual bool onTLSConnect(const gloox::CertInfo& info)
	{
		return true;
	}

	virtual void handleMUCParticipantPresence(gloox::MUCRoom *, const gloox::MUCRoomParticipant participant, const gloox::Presence& presence)
	{
		xmppLink.NickMap(participant.nick->resource(), participant.jid->username());
	}

	// Received something from someone in the room.
	virtual void handleMUCMessage(gloox::MUCRoom*, const gloox::Message& msg, bool priv)
	{
		// If when() is true, the message is being replayed from history; ignore it.
		// Also ignore messages from the bot itself.
		if (!msg.when() && msg.from().resource().compare(XMPPLINK_NICKNAME))
		{
			if (msg.body().compare("!ping"))
				xmppLink.Send(msg.from().resource(), XMPPLINK_CHATROOM, msg.body());
			else
				m_room->send(xmppLink.Ping(msg.from().resource()));
		}
	}

	virtual void handleMUCSubject(gloox::MUCRoom *, const std::string& nick, const std::string& subject)
	{
	}

	virtual void handleMUCError(gloox::MUCRoom *, gloox::StanzaError error)
	{
	}

	virtual void handleMUCInfo(gloox::MUCRoom *, int features, const std::string& name, const gloox::DataForm* infoForm)
	{
	}

	virtual void handleMUCItems(gloox::MUCRoom *, const gloox::Disco::ItemList& items)
	{
	}

	virtual void handleMUCInviteDecline(gloox::MUCRoom *, const gloox::JID& invitee, const std::string& reason)
	{
	}

	// Create the room if it doesn't exist.
	virtual bool handleMUCRoomCreation(gloox::MUCRoom *room)
	{
		return true;
	}

private:
	gloox::Client *j;
	gloox::MUCRoom *m_room;
	XmppLink xmppLink;
};

int main()
{
	ParagonBot *r = new ParagonBot();

	r->start();

	delete(r);
	return 0;
}
