package pcl.bridgebot.chatserverlink;

public class InvalidPackedMessageException extends Exception {

    private static final long serialVersionUID = 7143849167821708297L;

    public InvalidPackedMessageException(String stepFailed, int byteFailed) {
        super(String.format("Byte %04X : %s", byteFailed, stepFailed));
    }
}