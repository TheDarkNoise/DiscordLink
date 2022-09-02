package pcl.bridgebot.webserver.pages;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

public class WebPageContentsSingleton {
    private static String html = null;
    private static String navbar = null;

    public static String getNavigationBar() {
        if(navbar == null) {
            navbar = "<div class=\"innertube\"><h1><a href=\"channels\">Channels</a></h1></div>";
            navbar += "<div class=\"innertube\"><h1><a href=\"users\">Users</a></h1></div>";
            navbar += "<div class=\"innertube\"><h1><a href=\"settings\">Settings</a></h1></div>";
            navbar += "<div class=\"innertube\"><h1><a href=\"sendmsg\">SendMessage</a></h1></div>";
        }
        return navbar;
    }

    public static String getWebpageContents() throws IOException  {
        if(html == null) {
            html = new WebPageContentsSingleton().getWebpageContentsInternal();
        }
        return html;
    }

    public String getWebpageContentsInternal() throws IOException  {
        InputStream htmlIn = getClass().getResourceAsStream("/html/discord.html");
        return CharStreams.toString(new InputStreamReader(htmlIn, Charsets.UTF_8));
    }
}