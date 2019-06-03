package pcl.bridgebot.webserver.pages;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import pcl.bridgebot.webserver.HTTPDatabaseAdapter;
import pcl.bridgebot.webserver.HTTPDatabaseAdapter.DatabaseAdapterChannelResult;

public class WWWChannelList implements HttpHandler {
	private HTTPDatabaseAdapter databaseAdapter;

	public WWWChannelList(HTTPDatabaseAdapter databaseAdapter) {
		this.databaseAdapter = databaseAdapter;
	}

	@Override
	public void handle(HttpExchange t) throws IOException {
		String target = t.getRequestURI().toString();
		String response = "";

		String channelList = null;

		channelList = "<table><form method=\"get\" action=\"/\">";
		channelList = channelList + "<tr><td>Secret:</td><td><input type='text' name='secret' id='secret'></td></tr>";
		channelList = channelList + "<tr><td>Global Channel:</td><td><input type='text' name='gname'></td></tr>";
		channelList = channelList
				+ "<tr><td>Discord Channel ID:</td><td><input type='text' name='discordid'></td></tr>";
		channelList = channelList + "<input type='hidden' name='action' value='addChan'>";
		channelList = channelList + "<tr><td></td><td><input type='submit'></td></tr>";
		channelList = channelList + "</form></table><br><br>";

		channelList = channelList + "<table>";
		channelList = channelList + "<tr><th>Global Channel</th><th>Discord Channel</th><th>Manage</th></tr>";
		try {
			for (DatabaseAdapterChannelResult channel : databaseAdapter.getAllChannels()) {
				channelList += "<tr>";
				channelList += "<td>" + channel.inGameChannelName + "</td>";
				channelList += "<td>";
				channelList += "" + channel.discordServerName;
				channelList += " #" + channel.discordChannelName;
				channelList += " (id: " + channel.discordID + ")";
				channelList += "</td>";
				channelList += "<td><a href=\"#\" title=\"delete\" onclick=\"this.href='/?secret=' + document.getElementById('secret').value + '&action=delChan&discordid="
						+ channel.discordID + "'\">[Delete]</a></td></tr>";
			}
		} catch (Exception e) {
			System.out.println(e);
		}
		channelList = channelList + "</table>";

		// convert String into InputStream
		InputStream is = new ByteArrayInputStream(WebPageContentsSingleton.getWebpageContents().getBytes());
		try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
			String line = null;
			while ((line = br.readLine()) != null) {
				response = response + line.replace("#BODY#", target)
						.replace("#NAVIGATION#", WebPageContentsSingleton.getNavigationBar())
						.replace("#DATA#", channelList) + "\n";
			}
		}
		t.sendResponseHeaders(200, response.getBytes().length);
		OutputStream os = t.getResponseBody();
		os.write(response.getBytes());
		os.close();
	}
}
