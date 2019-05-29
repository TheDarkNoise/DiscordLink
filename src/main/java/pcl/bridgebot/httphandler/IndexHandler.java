package pcl.bridgebot.httphandler;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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

public class IndexHandler implements HttpHandler {
	static String html;
	public IndexHandler() throws IOException {
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

		PreparedStatement addChannel = null;
		PreparedStatement delChannel = null;
		PreparedStatement addUser = null;
		try {
			addChannel = Database.getPreparedStatement("addChannel");
			delChannel = Database.getPreparedStatement("removeChannel");
			addUser = Database.getPreparedStatement("addUser");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		List<NameValuePair> paramsList = URLEncodedUtils.parse(t.getRequestURI(),"utf-8");

        if (paramsList.size() > 1) {
        	String query = t.getRequestURI().toString().split("\\?")[1];
        	final Map<String, String> map = Splitter.on('&').trimResults().withKeyValueSeparator("=").split(query);		

			if (map.get("secret").contentEquals(HeroOne.httpdSecret)) {
				if (map.get("action").equals("addChan")) {
					try {
						String gname = java.net.URLDecoder.decode(map.get("gname"), StandardCharsets.UTF_8.name());
						addChannel.setString(1, gname);
						addChannel.setString(2, map.get("discordid"));
						addChannel.execute();
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else if (map.get("action").equals("delChan")) {
					try {
						delChannel.setString(1, map.get("gname"));
						delChannel.execute();
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else if (map.get("action").equals("addUser")) {
					try {
						addUser.setString(1, map.get("gname"));
						addUser.setString(2, map.get("discordid"));
						addUser.execute();
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}	
		}
		
		String bodyText ="Discord!";
		
		InputStream is = new ByteArrayInputStream(html.getBytes());
		try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
			String line = null;
			while ((line = br.readLine()) != null) {
				response = response + line.replace("#BODY#", target).replace("#NAVIGATION#", navData).replace("#DATA#", bodyText)+"\n";
			}
		}
		t.sendResponseHeaders(200, response.getBytes().length);
		OutputStream os = t.getResponseBody();
		os.write(response.getBytes());
		os.close();
	}
}
