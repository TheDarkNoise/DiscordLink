package pcl.bridgebot.httphandler;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.commons.codec.Charsets;

import com.google.common.io.CharStreams;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import pcl.HeroOne.util.Database;
import pcl.bridgebot.DiscordLink;

public class UserListHandler implements HttpHandler {
	
	static String html;
	public UserListHandler() throws IOException {
		InputStream htmlIn = getClass().getResourceAsStream("/html/discord.html");
		html = CharStreams.toString(new InputStreamReader(htmlIn, Charsets.UTF_8));
	}
	@Override
	public void handle(HttpExchange t) throws IOException {

		String target = t.getRequestURI().toString();
		String response = "";

		String navData = "";
	    navData += "<div class=\"innertube\"><h1><a href=\"channels\">Channels</a></h1></div>";
	    navData += "<div class=\"innertube\"><h1><a href=\"users\">Users</a></h1></div>";

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
	    PreparedStatement getAllUsers;
		try {
			getAllUsers = Database.getPreparedStatement("getAllUsers");
			ResultSet results = getAllUsers.executeQuery();
			while (results.next()) {
				userList = userList + "<tr><td>"+results.getString(1)+"</td><td>"+ DiscordLink.jda.getUserById(results.getString(2)).getName() +"/" + results.getString(2) +"</td><td><a href=\"#\"" + 
						"    title=\"delete\" onclick=\"this.href='/?secret=' + document.getElementById('secret').value + '&action=delUser&discordid=" + results.getString(2) + "'\">[Delete]</a></td></tr>";
			}
		} catch (Exception e) {
			System.out.println(e);
		}
	    userList = userList + "</table>";
		// convert String into InputStream
		InputStream is = new ByteArrayInputStream(html.getBytes());
		try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
			String line = null;
			while ((line = br.readLine()) != null) {
				response = response + line.replace("#BODY#", target).replace("#NAVIGATION#", navData).replace("#DATA#", userList)+"\n";
			}
		}
		t.sendResponseHeaders(200, response.getBytes().length);
		OutputStream os = t.getResponseBody();
		os.write(response.getBytes());
		os.close();
	}
}