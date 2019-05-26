package pcl.HeroOne.util;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import pcl.bridgebot.HeroOne;

@SuppressWarnings("restriction")
public class httpd {
	static HttpServer server;
	static String baseDomain;
	public static Map<String, String> pages = new LinkedHashMap<String, String>();
	public static void setup() throws Exception {
        server = HttpServer.create(new InetSocketAddress(HeroOne.httpdPort), 0);
		registerContext("/", new IndexHandler(), "Home");
	}
    /**
     * Creates a route from a URL to a HttpHandler
     * @param route
     * @param handlerIn
     * @param pageName
     */
	public static void registerContext(String route,  HttpHandler handlerIn, String pageName) {
		if(server != null) {
			System.out.println("Adding " + pageName + " to page list");
			pages.put(pageName, route);
			server.createContext(route, handlerIn);
		}
    }
    
	public static void start() throws Exception {
		if(server != null) {
			System.out.println("Starting HTTPD On port " + HeroOne.httpdPort);
			server.setExecutor(null); // creates a default executor
	        server.start();
		} else {
			System.out.println("httpd server was null!");
		}
    }
	public static void setBaseDomain(String httpdBaseDomain) {
		baseDomain = httpdBaseDomain;
	}
	
	public static String getBaseDomain() {
		return baseDomain;
	}
	
	static class IndexHandler implements HttpHandler {
		
		static String html;
		@Override
		public void handle(HttpExchange t) throws IOException {
			String response = "Hello!";
			t.sendResponseHeaders(200, response.getBytes().length);
			OutputStream os = t.getResponseBody();
			os.write(response.getBytes());
			os.close();
		}
	}
}