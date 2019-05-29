package pcl.bridgebot.chatserverlink;

public interface IUTF8MessageSplitter {
	public Iterable<String> splitMessage(String message);
}