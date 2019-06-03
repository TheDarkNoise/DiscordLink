package pcl.bridgebot.webserver.pages;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import pcl.bridgebot.database.CustomSettings;
import pcl.bridgebot.database.DatabaseHandler;

public class WWWCustomSettings implements HttpHandler {
	private DatabaseHandler database;

	public WWWCustomSettings(DatabaseHandler database) {
		this.database = database;
	}

	@Override
	public void handle(HttpExchange t) throws IOException {

		String target = t.getRequestURI().toString();
		String response = "";

		CustomSettings settings = database.getCustomSettings();

		String settingText = null;
		settingText = "<table><form method=\"get\" action=\"/\">";
		settingText += "<tr><td>Secret:</td><td><input type='text' name='secret' id='secret'></td></tr>";
		settingText += "<tr><td>Webhook username displayed as</td><td><select name='format'>";
		settingText += getFormatOption(settings.getFormatterMode(), "InGame", "In-game username");
		settingText += getFormatOption(settings.getFormatterMode(), "Discord", "Discord username");
		settingText += getFormatOption(settings.getFormatterMode(), "Mixed", "Discord username (In-game username)");
		settingText += "</select></td></tr>";
		settingText += "<tr><td>WebHook to use:</td><td><input type='text' name='webhook' value='"
				+ settings.getDefaultWebhookName() + "'></td></tr>";
		settingText += "<input type='hidden' name='action' value='updateSettings'>";
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

	private String getFormatOption(String currentMode, String requestedMode, String userReadableRequestedMode) {
		return "<option value=\"" + requestedMode + "\""
				+ (currentMode.equals(requestedMode) ? " selected=\"selected\"" : "") + ">" + userReadableRequestedMode
				+ "</option>";
	}
}