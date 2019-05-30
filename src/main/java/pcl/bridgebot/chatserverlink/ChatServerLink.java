package pcl.bridgebot.chatserverlink;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import pcl.bridgebot.chatserverlink.impl.PackedMessageDataFactory;
import pcl.bridgebot.chatserverlink.impl.UTF8MessageSplitter;

/**
 * A DLInk is a class allowing to connect the the CoH ChatServer
 */
public class ChatServerLink {
	private static final int MESSAGE_SIZE_LIMIT_IN_BYTES = 255;

	private String hostName;
	private int portNumber;
	private ConcurrentLinkedQueue<IPackedMessageData> pendingMessageList = new ConcurrentLinkedQueue<IPackedMessageData>();
	private boolean shouldRun = false;
	private IPackedMessageDataFactory packedMessageFactory;
	private IUTF8MessageSplitter messageSplitter;

	public ChatServerLink(String hostName, int portNumber) {
		// TODO switch to proper DI for these two classes
		this.packedMessageFactory = new PackedMessageDataFactory();
		this.messageSplitter = new UTF8MessageSplitter(MESSAGE_SIZE_LIMIT_IN_BYTES);
		this.hostName = hostName;
		this.portNumber = portNumber;
	}

	public void sendMessage(String chatroom, int userId, String userNickname, String message) {
		for (String messageFragment : messageSplitter.splitMessage(message)) {
			pendingMessageList
					.add(packedMessageFactory.getPackedMessageData(chatroom, userId, userNickname, messageFragment));
		}
	}

	/**
	 * Starts the main loop of the chat server listener. This method will block
	 * until the server is stoppped. Call the {@link ChatServerLink#close()} method
	 * to stop the server
	 * 
	 * <p>
	 * Usage : <blockquote>
	 * 
	 * <pre>
	 * link.startLoop(newMessage -> {
	 * 		Code to handle the newMessage packet
	 * })
	 * </pre>
	 * 
	 * </blockquote>
	 * 
	 * @param onMessageReceived The handler on message
	 * @throws ServerAlreadyRunningException if the link already has a running loop
	 */
	public void startLoop(Consumer<IPackedMessageData> onMessageReceived) throws ServerAlreadyRunningException {
		if (shouldRun) {
			throw new ServerAlreadyRunningException();
		}

		shouldRun = true;
		try {
			// The message buffer is 1000 bytes long, which should handle any message length
			// (the max message length should be around 770-780)
			byte[] buffer = new byte[1000];

			// Initialize the socket
			Socket clientSocket = new Socket(hostName, portNumber);
			InputStream inFromServer = clientSocket.getInputStream();
			OutputStream outToServer = clientSocket.getOutputStream();

			while (shouldRun) {
				// Send all the pending messages
				while (true) {
					// Get the next message to send
					IPackedMessageData pending = pendingMessageList.poll();
					// If there's nothing to send, break (we don't have anything to do)
					if (pending == null) {
						break;
					}
					// Send the pending message
					try {
						byte[] dataToSend = pending.getPackedMessage();
						outToServer.write(dataToSend, 0, dataToSend.length);
					} catch (Exception e) {
						// NO OP (server was closed)
						System.out.println("Issue while sending packet : " + e);
					}
				}

				// Create a client "in" socket
				// Check if the server has bytes to send
				if (inFromServer.available() <= 0) {
					// If not, wait a bit of time (250ms is usually good).
					TimeUnit.MILLISECONDS.sleep(250);
					continue;
				}

				// Read from the "in" socket until we get a message
				// This will be interrupted if there's a socket close event
				int dataRead = inFromServer.read(buffer);
				if (dataRead > 0) {
					try {
						// Read & accept the message
						IPackedMessageData result = packedMessageFactory.getPackedMessageData(buffer, dataRead);
						onMessageReceived.accept(result);
					} catch (Exception e) {
						// NO OP (server was closed)
						System.out.println("Issue while receiving packet : " + e);
					}
				}
			}

			// Close the socket (and exit the server listener)
			clientSocket.close();
		} catch (Exception e) {
			// TODO proper error handling for the socket stuff
			System.out.println("Start Loop threw: " + e);
		}
	}

	/**
	 * Closes any runnng loop on the link.
	 * 
	 * @param onMessageReceived The handler on message
	 */
	public void close() {
		// Stop the loop
		this.shouldRun = false;
	}
}