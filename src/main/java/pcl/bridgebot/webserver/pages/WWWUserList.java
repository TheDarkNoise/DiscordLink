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
import pcl.bridgebot.webserver.HTTPDatabaseAdapter.DatabaseAdapterUserResult;

public class WWWUserList implements HttpHandler {
	private HTTPDatabaseAdapter databaseAdapter;

	public WWWUserList(HTTPDatabaseAdapter databaseAdapter) {
		this.databaseAdapter = databaseAdapter;
	}

	@Override
	public void handle(HttpExchange t) throws IOException {

		String target = t.getRequestURI().toString();
		String response = "";

		String userList = null;

		userList = "<table><form method=\"get\" action=\"/\">";
		userList = userList + "<tr><td>Secret:</td><td><input type='text' name='secret' id='secret'></td></tr>";
		userList = userList + "<tr><td>Global ID:</td><td><input type='text' name='gname'></td></tr>";
		userList = userList + "<tr><td>Discord User ID:</td><td><input type='text' name='discordid'></td></tr>";
		userList = userList + "<input type='hidden' name='action' value='addUser'>";
		userList = userList + "<tr><td></td><td><input type='submit'></td></tr>";
		userList = userList + "</form></table><br><br>";

		userList = userList + "<table>";
		userList = userList + "<tr><th>Global ID</th><th>Discord Name</th><th>Manage</th></tr>";
		try {
			for (DatabaseAdapterUserResult user : databaseAdapter.getAllUsers()) {

				userList += "<tr>";
				userList += "<td>" + user.inGameId + "</td>";
				userList += "<td>" + user.discordName + " (id: " + user.discordID + ")</td>";
				userList += "<td><a href=\"#\" title=\"delete\" onclick=\"this.href='/?secret=' + document.getElementById('secret').value + '&action=delUser&discordid="
						+ user.discordID + "'\">[Delete]</a></td></tr>";
			}
		} catch (Exception e) {
			System.out.println(e);
		}
		userList = userList + "</table>";
		// convert String into InputStream
		InputStream is = new ByteArrayInputStream(WebPageContentsSingleton.getWebpageContents().getBytes());
		try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
			String line = null;
			while ((line = br.readLine()) != null) {
				response = response + line.replace("#BODY#", target)
						.replace("#NAVIGATION#", WebPageContentsSingleton.getNavigationBar())
						.replace("#DATA#", userList) + "\n";
			}
		}
		t.sendResponseHeaders(200, response.getBytes().length);
		OutputStream os = t.getResponseBody();
		os.write(response.getBytes());
		os.close();
	}
}