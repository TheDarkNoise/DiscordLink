package pcl.bridgebot.httphandler;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.Charsets;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import com.google.common.base.Splitter;
import com.google.common.io.CharStreams;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import pcl.HeroOne.util.Database;
import pcl.bridgebot.HeroOne;

public class ChannelListHandler implements HttpHandler {
	
	static String html;
	public ChannelListHandler() throws IOException {
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
	    String channelList = null;      
	    
	    channelList = "<table><form method=\"get\" action=\"/\">";
	    channelList = channelList + "<tr><td>Secret:</td><td><input type='text' name='secret' id='secret'></td></tr>";
	    channelList = channelList + "<tr><td>Global Channel:</td><td><input type='text' name='gname'></td></tr>";
	    channelList = channelList + "<tr><td>Discord Channel ID:</td><td><input type='text' name='discordid'></td></tr>";
	    channelList = channelList + "<input type='hidden' name='action' value='addChan'>";
	    channelList = channelList + "<tr><td></td><td><input type='submit'></td></tr>";
	    channelList = channelList + "</form></table><br><br>";
	    channelList = channelList + "<table>";
	    PreparedStatement getAllChannels;
		try {
			getAllChannels = Database.getPreparedStatement("getAllChannels");
			ResultSet results = getAllChannels.executeQuery();
			while (results.next()) {
				channelList = channelList + "<tr><td>"+results.getString(1)+"</td><td>"+ HeroOne.jda.getTextChannelById(results.getString(2)).getName() +"/" + results.getString(2) +"</td><td><a href=\"#\"" + 
						"    title=\"delete\" onclick=\"this.href='/?secret=' + document.getElementById('secret').value + '&action=delChan&gname=" + results.getString(1) + "'\">[Delete]</a></td></tr>";
			}
		} catch (Exception e) {
			System.out.println(e);
		}
		channelList = channelList + "</table>";

		// convert String into InputStream
		InputStream is = new ByteArrayInputStream(html.getBytes());
		try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
			String line = null;
			while ((line = br.readLine()) != null) {
				response = response + line.replace("#BODY#", target).replace("#NAVIGATION#", navData).replace("#DATA#", channelList)+"\n";
			}
		}
		t.sendResponseHeaders(200, response.getBytes().length);
		OutputStream os = t.getResponseBody();
		os.write(response.getBytes());
		os.close();
	}
}
