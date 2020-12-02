package pcl.bridgebot.webserver;

import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import pcl.bridgebot.DiscordLink;
import pcl.bridgebot.database.DatabaseHandler;
import pcl.bridgebot.discordserverlink.DiscordServerLink;
import pcl.bridgebot.webserver.pages.WWWChannelList;
import pcl.bridgebot.webserver.pages.WWWCustomSettings;
import pcl.bridgebot.webserver.pages.WWWIndex;
import pcl.bridgebot.webserver.pages.WWWUserList;

public class HTTPd {
	private HttpServer server;
	private String baseDomain;
	private Map<String, String> pages = new LinkedHashMap<String, String>();

	public void setup(int port) throws Exception {
		server = HttpServer.create(new InetSocketAddress(port), 0);
	}

	/**
	 * Creates a route from a URL to a HttpHandler
	 * 
	 * @param route
	 * @param handlerIn
	 * @param pageName
	 */
	public void registerContext(String route, HttpHandler handlerIn, String pageName) {
		if (server != null) {
			System.out.println("Adding " + pageName + " to page list");
			pages.put(pageName, route);
			server.createContext(route, handlerIn);
		}
	}

	public void start(int port) {
		if (server != null) {
			System.out.println("Starting HTTPD On port " + port);
			server.setExecutor(null); // creates a default executor
			server.start();
			System.out.println("Please visit http://127.0.0.1:" + port + " To configure the bridge");
		} else {
			System.out.println("httpd server was null!");
		}
	}

	public void setBaseDomain(String httpdBaseDomain) {
		baseDomain = httpdBaseDomain;
	}

	public String getBaseDomain() {
		return baseDomain;
	}

	public void registerPages(String httpdSecret, DatabaseHandler databaseHandler,
			DiscordServerLink discordServerLink) {
		HTTPDatabaseAdapter adapter = new HTTPDatabaseAdapter(databaseHandler, discordServerLink);
		registerContext("/", new WWWIndex(databaseHandler, httpdSecret), "Discord");
		registerContext("/channels", new WWWChannelList(adapter), "Channels");
		registerContext("/users", new WWWUserList(adapter), "Users");
		registerContext("/settings", new WWWCustomSettings(databaseHandler), "Settings");
	}
}