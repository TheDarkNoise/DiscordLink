package pcl.bridgebot;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * A DLInk is a class allowing to connect the the CoH ChatServer
 */
public class ChatServerLink 
{
	private String hostName;
	private int portNumber;
	private ConcurrentLinkedQueue<PackedMessageData> pendingMessageList = new ConcurrentLinkedQueue<PackedMessageData>();
	private boolean shouldRun = true;

	public ChatServerLink(String hostName, int portNumber) {
		this.hostName = hostName;
		this.portNumber = portNumber;
	}

	public void sendMessage(String chatroom, int userId, String userNickname, String message) {
		// Create the message to send to the server
		PackedMessageData newMessage = new PackedMessageData();
		newMessage.chatroom = chatroom;
		newMessage.userId = userId;
		newMessage.userNickname = userNickname;
		newMessage.message = message;
		// Store it as "pending message"
		pendingMessageList.add(newMessage);
	}

	public void startLoop(Consumer<PackedMessageData> onMessageReceived) {
		shouldRun = true;
		try {
			byte[] buffer = new byte[1000];
			Socket clientSocket = new Socket(hostName, portNumber);
			DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
			while(shouldRun) {
				if(clientSocket == null) {
				}
				// Send all the pending messages
				while(true) {
					// Get the next message to send
					PackedMessageData pending = pendingMessageList.poll();
					// If there's nothing to send, break (we don't have anything to do)
					if(pending == null)
						break;
					// Send the pending message
					byte[] dataToSend = pending.getPackedMessage();
					outToServer.write(dataToSend);
				}
				// Create a client "in" socket
				InputStream inFromServer = clientSocket.getInputStream();
				try {
					// Check if the server has bytes to send
					if(inFromServer.available() <= 0)
					{
						// If not, wait a bit of time.
						TimeUnit.MILLISECONDS.sleep(500);
						continue;
					}
					// Read from the "in" socket until we get a message
					// This will be interrupted if there's a socket close event
					int dataRead = inFromServer.read(buffer);
					if(dataRead > 0) {
						// Read & accept the message
						PackedMessageData result = PackedMessageData.getDataFromPackedMessage(buffer, dataRead);
						onMessageReceived.accept(result);
					}
				}
				catch(IOException e) {
					// NO OP (server was closed)
				}
			}
			clientSocket.close();
		}
		catch(Exception e) {
			// TODO proper error handling for the socket stuff
			System.out.println(e.getMessage());
		}
	}

	public void close() {
		// Stop the loop
		this.shouldRun = false;
	}

	public static class PackedMessageData
	{
		public int userId;
		public String chatroom;
		public String message;
		public String userNickname;
		public MESSAGE_TYPE type;

		public byte[] getPackedMessage() {
			return PackedMessageData.getPackedMessageFromData(
				type, userId, chatroom, message, userNickname
			);
		}

		public static byte[] getPackedMessageFromData(MESSAGE_TYPE messageType, int userId, String chatroom, String message, String usernickname) {
			if(messageType == MESSAGE_TYPE.MOTD) {
				throw new IllegalArgumentException();
			}

			/* Standard Client=>Server Packed message format :
				Int16  Packet size.
				Int8   Message type;
					0x64 is "Send to Channel".
				Int32  User ID.
				PascalStr (Int8 + Char[])
					Chat room name
				PascalStr (Int8 + Char[])
					Message.
				PascalStr (Int8 + Char[])
					Nickname (ignored by the server, sent for completeness)
			*/
			int messageSize = 2 + 1 + 4 // Message head (packet size, message type, user ID)
				+ chatroom.length() + 1 // Pascal string for chatroom
				+ message.length() + 1 // Pascal string for message
				+ usernickname.length() + 1; // Pascal string for nickname
			ByteBuffer buffer = ByteBuffer.allocate(messageSize);
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			// Add the packet size
			buffer.putShort((short) messageSize);
			// Add the MESSAGE_TYPE value (0x64)
			buffer.put((byte) 0x64);
			// Add the User ID
			buffer.putInt(userId);
			// 
			buffer.put((byte) chatroom.length());
			buffer.put(Arrays.copyOf(chatroom.getBytes(), chatroom.length()));
			buffer.put((byte) message.length());
			buffer.put(Arrays.copyOf(message.getBytes(StandardCharsets.UTF_8), message.length()));
			buffer.put((byte) usernickname.length());
			buffer.put(Arrays.copyOf(usernickname.getBytes(), usernickname.length()));
			return buffer.array();
		}
	
		public static PackedMessageData getDataFromPackedMessage(byte[] packedMessage, int announcedSize) {
			
			ByteBuffer buffer = ByteBuffer.wrap(packedMessage);
			buffer.order(ByteOrder.LITTLE_ENDIAN);

			// Get packet size (for checking)
			short packetSize = buffer.getShort();
			assert packetSize == announcedSize;
	
			PackedMessageData result = new PackedMessageData();
			byte messageTypeId = buffer.get();
			if(messageTypeId == 0x64) {
				/* Standard Sever=>Client Packed message format :
					Int16  Packet size.
					Int8   Message type;
						0x64 is "Send to Channel".
					Int32  User ID.
					PascalStr (Int8 + Char[])
						Chat room name
					PascalStr (Int8 + Char[])
						Message.
					PascalStr (Int8 + Char[])
						Nickname
				*/
				result.type = MESSAGE_TYPE.CHANNEL;
	
				result.userId = buffer.getInt();
		
				// Pascal string for chatroom
				byte chatroomLength = buffer.get();
				byte[] chatroomRaw = new byte[chatroomLength];
				buffer.get(chatroomRaw);
				result.chatroom = new String(chatroomRaw);
		
				// Pascal string for message
				byte messageLength = buffer.get();
				byte[] messageRaw = new byte[messageLength];
				buffer.get(messageRaw);
				result.message = new String(messageRaw);
		
				// Pascal string for nickname
				byte nicknameLength = buffer.get();
				byte[] nicknameRaw = new byte[nicknameLength];
				buffer.get(nicknameRaw);
				result.userNickname = new String(nicknameRaw);
			}
			else if(messageTypeId == 0x65) {
				/* MOTD Sever=>Client Packed message format :
					Int16  Packet size.
					Int8   Message type;
						0x65 is "Message of the Day".
					PascalStr (Int8 + Char[])
						Chat room name
					PascalStr (Int8 + Char[])
						Message
				*/
				result.type = MESSAGE_TYPE.MOTD;
		
				// Pascal string for chatroom
				byte chatroomLength = buffer.get();
				byte[] chatroomRaw = new byte[chatroomLength];
				buffer.get(chatroomRaw);
				result.chatroom = new String(chatroomRaw);
		
				// Pascal string for message
				byte messageLength = buffer.get();
				byte[] messageRaw = new byte[messageLength];
				buffer.get(messageRaw);
				result.message = new String(messageRaw);
		
			}
			return result;
		}
	
		public enum MESSAGE_TYPE
		{
			CHANNEL,
			MOTD,
		}
	}
}