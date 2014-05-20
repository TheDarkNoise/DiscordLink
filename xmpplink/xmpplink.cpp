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
#include "gloox/src/loghandler.h"
#include "gloox/src/logsink.h"

#include <stdio.h>
#include <locale.h>
#include <string>
#include <cstdio>
#include <windows.h>

#define XMPPLINK_PORT 31415

#define XMPPLINK_STATE_NONE 0
#define XMPPLINK_STATE_READY 2

#define XMPPLINK_COMMAND_CHANSEND 100

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
		addr.sin_addr.s_addr = inet_addr("127.0.0.1");
		addr.sin_port = htons(XMPPLINK_PORT);

		if (connect(sockfd, (struct sockaddr *)&addr, sizeof(addr)) < 0)
		{
			printf("XMPP Link Connection Error: %d\n", WSAGetLastError());
			return false;
		}

		state = XMPPLINK_STATE_READY;
		return true;
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

					// Next in buffer is the User ID of the sender of the message.
					memcpy(&user_id, &buffer[3], 4);

					// And next, the length of the chat channel.
					len = buffer[7];
					dest.assign(&buffer[8], len);

					len = buffer[8 + len];
					message.assign(&buffer[8 + 13], len);
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

	int user_id;
	std::string dest;
	std::string message;

private:
	SOCKET sockfd;
	fd_set fd;
	int state;
	char buffer[1000];
	int size;
	int reconnectTime;
	struct timeval timeout;
};

class ParagonBot : public gloox::ConnectionListener, gloox::MUCRoomHandler
{
public:
	ParagonBot() {}

	virtual ~ParagonBot() {}

	void start()
	{
		gloox::JID jid("bot@xmpp.example.com/Github");
		j = new gloox::Client(jid, "password");
		j->registerConnectionListener(this);
		j->setPresence(gloox::Presence::Available, -1);
		j->disco()->setVersion("ParagonBot", "0.01", "Windows");
		j->disco()->setIdentity("client", "bot", "Chat Bot");
		j->setCompression(false);
		gloox::StringList ca;
		ca.push_back("/path/to/cacert.crt");
		j->setCACerts(ca);

		gloox::JID nick("chat@chat.xmpp.rpcongress.com/Bot");
		m_room = new gloox::MUCRoom(j, nick, this, 0);

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
					m_room->send(std::to_string(xmppLink.user_id) + ": " + xmppLink.message);
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
		printf("Disconnedted from XMPP, retrying...\n");
	}

	virtual bool onTLSConnect(const gloox::CertInfo& info)
	{
		return true;
	}

	virtual void handleMUCParticipantPresence(gloox::MUCRoom *, const gloox::MUCRoomParticipant participant, const gloox::Presence& presence)
	{
	}

	virtual void handleMUCMessage(gloox::MUCRoom*, const gloox::Message& msg, bool priv)
	{
		// Received something from someone in the room.
		// This is where the stuff to send data back to the chat server should go.
		// The message will only show the nickname as the sender, something needs
		// to be done to translate that to a chat server ID.
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

	virtual void handleMUCInviteDecline(gloox::MUCRoom * /*room*/, const gloox::JID& invitee, const std::string& reason)
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

	while (1)
	{
		// This should only end if disconnected, so reconnect right away.
		// The XmppLink probably won't be happy about this.
		r->start();
	}

	delete(r);
	return 0;
}
