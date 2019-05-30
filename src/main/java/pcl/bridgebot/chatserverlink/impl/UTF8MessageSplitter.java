package pcl.bridgebot.chatserverlink.impl;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import pcl.bridgebot.chatserverlink.IUTF8MessageSplitter;

public class UTF8MessageSplitter implements IUTF8MessageSplitter {

    private int sizeLimitInBytes;

    public UTF8MessageSplitter(int sizeLimitInBytes) {
        this.sizeLimitInBytes = sizeLimitInBytes;
    }

    public Iterable<String> splitMessage(String message) {
        ArrayList<String> pendingMessageList = new ArrayList<>();

        int totalMessageLength = message.getBytes(StandardCharsets.UTF_8).length;
        if (totalMessageLength <= sizeLimitInBytes) {
            // Store directly the whole message as "pending message"
            pendingMessageList.add(message);
            return pendingMessageList;
        }
        // Split the message into 255-byte long chunks of text, if possible by splitting
        // them on white spaces
        String[] messageChunks = message.split(" ");
        String aggregatedMessage = "";
        for (String chunk : messageChunks) {
            if (chunk.getBytes(StandardCharsets.UTF_8).length > sizeLimitInBytes) {
                // The chunk itself is too big. Split it in several smaller 255-bytes messages
                // and send that.
                // Store the previous chunk as "pending message"
                // Special case for the very first chunk.
                if(!aggregatedMessage.equals("")) {
                    pendingMessageList.add(aggregatedMessage);
                    aggregatedMessage = "";
                }
                // Note: to split properly (and not miss characters), we first need to split the
                // string into characters. Then we aggregate characters until done and send each
                // chunk.
                // It's kind of a smaller version of the overall loop, because this one can't
                // fail.
                String[] chunkCharacters = chunk.split("");
                for (String character : chunkCharacters) {
                    if (aggregatedMessage.concat(character)
                            .getBytes(StandardCharsets.UTF_8).length > sizeLimitInBytes) {
                        // Store the previous chunk as "pending message"
                        pendingMessageList.add(aggregatedMessage);
                        aggregatedMessage = "";
                    }
                    aggregatedMessage = aggregatedMessage.concat(character);
                }
                // Stop there. The remaining bits which aren't sent will start the next line and
                // be pushed with the next message
                continue;
            }
            if (aggregatedMessage.concat(" ").concat(chunk)
                    .getBytes(StandardCharsets.UTF_8).length > sizeLimitInBytes) {
                // The chunk cannot be added to the message
                // Store the previous aggregation as "pending message"
                pendingMessageList.add(aggregatedMessage);
                // Reset the aggregation
                aggregatedMessage = chunk;
                continue;
            }
            // Special case for the very first chunk.
            if(aggregatedMessage.equals("")) {
                aggregatedMessage = chunk;
                continue;
            }
            aggregatedMessage = aggregatedMessage.concat(" ").concat(chunk);
        }
        // Store the remaining chunk as "pending message"
        pendingMessageList.add(aggregatedMessage);
        return pendingMessageList;
    }
}