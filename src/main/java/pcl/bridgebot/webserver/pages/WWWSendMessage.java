package pcl.bridgebot.webserver.pages;

import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.Webhook;
import org.apache.http.NameValuePair;
import pcl.bridgebot.DiscordLink;
import pcl.bridgebot.discordserverlink.DiscordServerLink;
import pcl.bridgebot.webserver.HTTPDatabaseAdapter;
import pcl.bridgebot.webserver.QueryEndpointHandler;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.*;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Map.*;

public class WWWSendMessage  implements HttpHandler {


    private HTTPDatabaseAdapter databaseAdapter;

    public WWWSendMessage() {  }

    @Override
    public void handle(HttpExchange t) throws IOException {

        List<NameValuePair> params = URLEncodedUtils.parse(t.getRequestURI(), Charset.forName("UTF-8"));
        Map<String, String> map = Maps.newHashMap();

        for (NameValuePair param : params) {
            map.put(param.getName(), param.getValue());
        }



        String target = t.getRequestURI().toString();

        String response = "";

        String userList = null;

        userList = "<table><form method=\"get\" action=\"/sendmsg\">";
        userList = userList + "<tr><td>Name:</td><td><input type='text' name='name' id='name'></td></tr>";
        userList = userList + "<tr><td>Message:</td><td><input type='text' name='message'></td></tr>";
        userList = userList + "<tr><td>Discord Channel ID:</td><td><input type='text' name='chanid'></td></tr>";
        userList = userList + "<input type='hidden' name='action' value='sendMsg'>";
        userList = userList + "<tr><td></td><td><input type='submit'></td></tr>";
        userList = userList + "</form></table><br><br>";
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
        if (!map.isEmpty()) {
            Optional<String> discordUserId = Optional.empty();


            String nickName = map.get("name");
            String channelID = map.get("chanid");
            String message = URLDecoder.decode( map.get("message"), "UTF-8" );

            DiscordServerLink.sendMessageToChannel(discordUserId, nickName, message, channelID);
        }


        t.sendResponseHeaders(200, response.getBytes().length);
        OutputStream os = t.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

}