package pcl.bridgebot.chatserverlink;

public class ServerAlreadyRunningException extends Exception {

    private static final long serialVersionUID = -3243855820650264216L;

    public ServerAlreadyRunningException() {
        super("A loop was already started on this ChatServerLink instance");
    }
}