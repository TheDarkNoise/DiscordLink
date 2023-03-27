package pcl.bridgebot.webserver.pages;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import pcl.bridgebot.webserver.HTTPDatabaseAdapter;

import java.io.*;

public class WWWQuit implements HttpHandler {
	private HTTPDatabaseAdapter databaseAdapter;

	public WWWQuit() {
		this.databaseAdapter = databaseAdapter;
	}

	@Override
	public void handle(HttpExchange t) throws IOException {

		String target = t.getRequestURI().toString();
		String response = "";

		String userList = null;

		userList = "-";

		System.exit(0);

		t.sendResponseHeaders(200, response.getBytes().length);
		OutputStream os = t.getResponseBody();
		os.write(response.getBytes());
		os.close();
	}
}