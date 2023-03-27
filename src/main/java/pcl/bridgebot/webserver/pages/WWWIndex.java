package pcl.bridgebot.webserver.pages;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.base.Splitter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import pcl.bridgebot.database.AdminSettings;
import pcl.bridgebot.database.CustomSettings;
import pcl.bridgebot.database.DatabaseHandler;
import pcl.bridgebot.webserver.QueryEndpointHandler;

public class WWWIndex implements HttpHandler {
	private final String secret;

	private final ArrayList<QueryEndpointHandler> endpoints = new ArrayList<>();

	public WWWIndex(DatabaseHandler database, String secret) {
		this.secret = secret;

		registerQueryEndpoint(QueryEndpointHandler.create("Error while adding channel", "./channels",
				context -> context.get("action").equals("addChan"), context -> {
					String gname = null;
					try {
						gname = java.net.URLDecoder.decode(context.get("gname"), StandardCharsets.UTF_8.name());
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return database.handleAddChannelAction(gname, context.get("discordid"));
				}));
		registerQueryEndpoint(QueryEndpointHandler.create("Error while removing channel", "./channels",
				context -> context.get("action").equals("delChan"),
				context -> database.handleRemoveChannelAction(context.get("discordid"))));
		registerQueryEndpoint(QueryEndpointHandler.create("Error while adding user", "./users",
				context -> context.get("action").equals("addUser"),
				context -> database.handleAddUserAction(context.get("gname"), context.get("discordid"))));
		registerQueryEndpoint(QueryEndpointHandler.create("Error while removing user", "./users",
				context -> context.get("action").equals("delUser"),
				context -> database.handleRemoveUserAction(context.get("discordid"))));
		registerQueryEndpoint(QueryEndpointHandler.create("Error while removing user", "./quit",
				context -> context.get("action").equals("quit"), context -> {
						System.exit(0);
						return true;
				}));
		registerQueryEndpoint(QueryEndpointHandler.create("Error while updating settings", "./settings",
				context -> context.get("action").equals("updateSettings"), context -> {
					try {
						database.setCustomSettings(new CustomSettings(context.get("format"), context.get("webhook")));
						return true;
					} catch (Exception e) {
						return false;
					}
				}));
		registerQueryEndpoint(QueryEndpointHandler.create("Error while updating settings", "./chatadminsettings",
				context -> context.get("action").equals("updateAdminSettings"), context -> {
					try {
						database.setAdminSettings(new AdminSettings(context.get("adminchannel")));
						return true;
					} catch (Exception e) {
						return false;
					}
				}));
	}

	private void registerQueryEndpoint(QueryEndpointHandler endpoint) {
		endpoints.add(endpoint);
	}

	@Override
	public void handle(HttpExchange t) throws IOException {
		List<NameValuePair> paramsList = URLEncodedUtils.parse(t.getRequestURI(), StandardCharsets.UTF_8);

		if (paramsList.size() > 1) {
			handleQueryToServer(t);
			return;
		}

		String target = t.getRequestURI().toString();
		String response = "";
		String bodyText = "Discord!<br>";
		bodyText += "<form method=\"get\" action=\"/quit\"><input type='hidden' name='action' value='quit'><input type='submit' value='Quit Bot'></form>";

		InputStream is = new ByteArrayInputStream(WebPageContentsSingleton.getWebpageContents().getBytes());
		try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
			String line = null;
			while ((line = br.readLine()) != null) {
				response = response + line.replace("#BODY#", target)
						.replace("#NAVIGATION#", WebPageContentsSingleton.getNavigationBar())
						.replace("#DATA#", bodyText) + "\n";
			}
		}
		t.sendResponseHeaders(200, response.getBytes().length);
		OutputStream os = t.getResponseBody();
		os.write(response.getBytes());
		os.close();
	}

	private void handleQueryToServer(HttpExchange t) throws IOException {
		String query = t.getRequestURI().toString().split("\\?")[1];
		final Map<String, String> map = Splitter.on('&').trimResults().withKeyValueSeparator("=").split(query);

		String redirectPath = null;
		String errorMessage = null;
		if (map.get("secret").contentEquals(secret)) {
			// Find the right query endpoint to execute
			for (QueryEndpointHandler endpoint : endpoints) {
				if (endpoint.shouldExecute(map)) {
					boolean result = endpoint.execute(map);
					if (result) {
						redirectPath = endpoint.onSuccessRedirectPath();
					} else {
						errorMessage = endpoint.onFailureMessage();
					}
					break;
				}
			}
		} else {
			errorMessage = "The Secret was incorrect or missing!";
		}

		if (errorMessage != null) {
			String response = "<html><body>Error while executing request : " + errorMessage;
			t.sendResponseHeaders(400, response.getBytes().length);
			OutputStream os = t.getResponseBody();
			os.write(response.getBytes());
			os.close();
			return;
		}

		String response = "<html><head><meta http-equiv=\"refresh\" content=\"0; url=\\" + redirectPath
				+ "\" /></head><body>Request successful.";
		t.sendResponseHeaders(200, response.getBytes().length);
		OutputStream os = t.getResponseBody();
		os.write(response.getBytes());
		os.close();
		return;
	}
}
