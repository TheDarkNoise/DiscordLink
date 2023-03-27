package pcl.bridgebot.webserver.pages;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import pcl.bridgebot.database.AdminSettings;
import pcl.bridgebot.database.DatabaseHandler;

import java.io.*;

public class WWWChatAdminSettings implements HttpHandler {
	private DatabaseHandler database;

	public WWWChatAdminSettings(DatabaseHandler database) {
		this.database = database;
	}

	@Override
	public void handle(HttpExchange t) throws IOException {

		String target = t.getRequestURI().toString();
		String response = "";

		AdminSettings settings = database.getAdminSettings();
		String settingText = null;

		//settingText = "Hello! " + settings.getDefaultAdminChannel() + "<br>";
		settingText = "<table><form method=\"get\" action=\"/\">";
		settingText += "<tr><td>Secret:</td><td><input type='text' name='secret' id='secret'></td></tr>";
		settingText += "<tr><td>Admin channel:</td><td><input type='text' name='adminchannel' value='" + settings.getDefaultAdminChannel() + "'></td></tr>";
		settingText += "<input type='hidden' name='action' value='updateAdminSettings'>";
		settingText += "<tr><td></td><td><input type='submit' value='update'></td></tr>";
		settingText += "</form></table><br><br>";
		// convert String into InputStream
		InputStream is = new ByteArrayInputStream(WebPageContentsSingleton.getWebpageContents().getBytes());

		try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
			String line = null;
			while ((line = br.readLine()) != null) {
				response = response + line.replace("#BODY#", target)
						.replace("#NAVIGATION#", WebPageContentsSingleton.getNavigationBar())
						.replace("#DATA#", settingText) + "\n";
			}
		}
		t.sendResponseHeaders(200, response.getBytes().length);
		OutputStream os = t.getResponseBody();
		os.write(response.getBytes());
		os.close();
	}
}