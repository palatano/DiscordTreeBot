package tree.command.analysis;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tree.command.data.GoogleResults;
import tree.command.util.MessageUtil;
import tree.commandutil.CommandManager;
import tree.commandutil.type.AnalysisCommand;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import tree.util.LoggerUtil;

import javax.print.Doc;
import javax.xml.ws.http.HTTPException;

/**
 * Created by Valued Customer on 8/2/2017.
 */
public class GoogleSearchCommand implements AnalysisCommand {
    private String userAgent;
    private String commandName;
    private int counter = 0;
    private static Logger log = LoggerFactory.getLogger(CommandManager.class);

    public GoogleSearchCommand(String commandName) {
        this.commandName = commandName;
    }

    private void printResult(EmbedBuilder embed, Element link, Guild guild,
                             MessageChannel msgChan, Message message,
                             Member member, String[] args) throws Exception {
        String title = link.text();
        String url = link.absUrl("href");
        // Google returns URLs in format "http://www.google.com/url?q=<url>&sa=U&ei=<someKey>".
        System.out.println(url);
        System.out.println(url.substring(url.indexOf('=') + 1, url.indexOf('&')));
        url = URLDecoder.decode(url.substring(url.indexOf('=') + 1, url.indexOf('&')), "UTF-8");
        if (!url.startsWith("http")) {
            return;
        }
        URLConnection urlCon = new URL(url).openConnection();
        urlCon.setRequestProperty("User-Agent", userAgent);
        urlCon.connect();
        InputStream is = urlCon.getInputStream();
        String redirLink = urlCon.getURL().toString();

        Document doc = Jsoup.connect(redirLink).ignoreContentType(true).get();

        List<Element> list = doc.select("meta[name=description]");
        String description = list.size() == 0 ? doc.title() : list.get(0).attr("content");
        description = description.length() > 256 ? description.substring(0, 256) + "..." : description;
        description += "\n" + "**Link: **" + "<" + redirLink + ">";
        embed.addField(++counter + ") " + title, description, true);
    }

    private void performSearchQuery(Guild guild, MessageChannel msgChan,
                                    Message message, Member member, String[] args) {
        GoogleResults results = null;
        String google = "http://www.google.com/search?q="; //https://www.youtube.com/results?search_query=test
        String search = "";
        String charset = "UTF-8";
        userAgent = "TreeBot";
        if (args.length < 2) {
            MessageUtil.sendError("No arguments added to the search command.", msgChan);
            return;
        }
        for (int i = 1; i < args.length; i++) {
            search += args[i] + " ";
        }
        EmbedBuilder embed = new EmbedBuilder().setDescription("**Search results for \"" + search + "\": **");

        try {
            Elements links = Jsoup.connect(google +
                    URLEncoder.encode(search, charset) + "&safe=on")
                    .userAgent(userAgent)
                    .get()
                    .select(".g>.r>a");
            counter = 0;
            if (links.size() <= 0) {
                MessageUtil.sendError("No search results found for: " + search, msgChan);
                return;
            }
            for (Element link : links) {
                if (counter >= 3) {
                    break;
                }
                printResult(embed, link, guild, msgChan, message, member, args);
            }
        } catch (Exception e) {
            LoggerUtil.logError(e, log, message);
            e.printStackTrace();
            return;
        }
        msgChan.sendMessage(embed.build()).queue();
    }

    @Override
    public void execute(Guild guild, MessageChannel msgChan, Message message, Member member, String[] args) {
        performSearchQuery(guild, msgChan, message, member, args);
    }

    @Override
    public String help() {
        return "``" + CommandManager.botToken + commandName + " [search]``: Returns the first three search results from a google search query.";
    }

    @Override
    public String getCommandName() {
        return commandName;
    }
}
