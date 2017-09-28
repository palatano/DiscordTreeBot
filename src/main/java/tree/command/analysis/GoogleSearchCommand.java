package tree.command.analysis;

import com.google.api.services.youtube.model.SearchResult;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tree.command.data.GoogleResults;
import tree.command.data.MenuSelectionInfo;
import tree.command.data.MessageWrapper;
import tree.command.data.ReactionMenu;
import tree.command.util.MessageUtil;
import tree.commandutil.CommandManager;
import tree.commandutil.type.AnalysisCommand;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;

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
    private static Logger log = LoggerFactory.getLogger(CommandManager.class);
    private Map<Guild, Map<Long, MenuSelectionInfo>> guildToUserMap;
    private Map<Guild, Map<Long, Integer>> guildToIndexMap;
    public static final int MAX_RESULTS = 3;

    public GoogleSearchCommand(String commandName) {
        this.commandName = commandName;
        guildToUserMap = new HashMap<>();
        guildToIndexMap = new HashMap<>();
    }

    private void checkIfGuildExists(Guild guild) {
        if (!guildToUserMap.containsKey(guild)) {
            guildToUserMap.put(guild, new HashMap<>());
        }
        if (!guildToIndexMap.containsKey(guild)) {
            guildToIndexMap.put(guild, new HashMap<>());
        }
    }

    private void addSelectionEntry(Guild guild, MessageChannel msgChan, long userId,
                                   MenuSelectionInfo msInfo, int index) {
        guildToUserMap.get(guild).put(userId, msInfo);
        guildToIndexMap.get(guild).put(userId, index);
    }

    private void removeSelectionEntry(Guild guild, MessageChannel msgChan, long userId) {
        int index = guildToIndexMap.get(guild).remove(userId);
        MenuSelectionInfo msInfo = guildToUserMap.get(guild).remove(userId);
        if (index == -1 || msInfo == null) {
            MessageUtil.sendError("Error in " + commandName + ": Attempting to remove a non-existent user.", msgChan);
        }
        long menuId = msInfo.getMenu().getIdLong();
        if (menuId != 0) {
            msgChan.deleteMessageById(menuId).queue();
        }

        CommandManager.removeReactionMenu(guild, menuId);
    }

    public boolean isSelectingUser(Guild guild, Member member) {
        return guildToUserMap.get(guild).containsKey(member.getUser().getIdLong());
    }

    public boolean hasMenu(Guild guild, long userId) {
        if (!guildToUserMap.containsKey(guild)) {
            return false;
        }
        Map<Long, MenuSelectionInfo> userInfoMap = guildToUserMap.get(guild);
        if (userInfoMap.containsKey(userId)) {
            if (userInfoMap.get(userId).getMenu().getIdLong() == 0) {
                userInfoMap.remove(userId);
                System.out.println("Error, this should not happen.");
                return false;
            }
            return true;
        }
        return false;
    }

    public void nextOptionSelected(Guild guild, MessageChannel msgChan, Member member) {
        long userId = member.getUser().getIdLong();
        Map<Long, MenuSelectionInfo> menuSelectionInfoMap = guildToUserMap.get(guild);
        Map<Long, Integer> indexMap = guildToIndexMap.get(guild);
        int index = indexMap.get(userId);
        MenuSelectionInfo prevMsInfo = menuSelectionInfoMap.get(userId);

        boolean hasNextOption = true;
        if (++index >= MAX_RESULTS - 1) {
            hasNextOption = false;
        }
        guildToIndexMap.get(guild).put(userId, index);

        MessageWrapper msgWrapper = new MessageWrapper();
        List<Element> newList = null;
        try {
            newList = getNextResult(guild, member, msgChan, msgWrapper, hasNextOption);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (newList == null) {
            return;
        }

        // Delete the previous menu.
        if (hasMenu(guild, userId)) {
            removeSelectionEntry(guild, msgChan, member.getUser().getIdLong());
        }

        MenuSelectionInfo msInfo =
                new MenuSelectionInfo(msgWrapper.getMessage(), msgChan,
                        newList, null);
        addSelectionEntry(guild, msgChan, member.getUser().getIdLong(), msInfo, index);
        ReactionMenu menu = new ReactionMenu(commandName, userId, msgChan);
        CommandManager.addReactionMenu(guild,
                msgWrapper.getMessage().getIdLong(), menu);


    }

    String getMetaTag(Document document, String attr) {
        Elements elements = document.select("meta[name=" + attr + "]");
        for (Element element : elements) {
            final String s = element.attr("content");
            if (s != null) return s;
        }
        elements = document.select("meta[property=" + attr + "]");
        for (Element element : elements) {
            final String s = element.attr("content");
            if (s != null) return s;
        }
        return null;
    }


    private List<Element> getNextResult(Guild guild, Member member, MessageChannel msgChan,
                                       MessageWrapper msgWrapper, boolean hasNextOption) throws Exception {

        long userId = member.getUser().getIdLong();
        MenuSelectionInfo msInfo = guildToUserMap.get(guild).get(userId);
        long resultIndex = guildToIndexMap.get(guild).get(userId);
        if (msInfo == null) {
            return null;
        }

        List<Element> searchResultList =
                (List<Element>) msInfo.getSongsToChoose();

        if (searchResultList.isEmpty()) {
            System.out.println("No more results found.");
            MessageUtil.sendError("No more results found.", msgChan);
            return null;
        }

        EmbedBuilder embed = new EmbedBuilder();
        Iterator<Element> searchResultIterator = searchResultList.iterator();
        int index = 0;

        msgChan.sendTyping().queue();
        while (searchResultIterator.hasNext()) {
            Element link = searchResultIterator.next();
            index++;
            // Google returns URLs in format "http://www.google.com/url?q=<url>&sa=U&ei=<someKey>".

            String title = link.text();
            String url = link.absUrl("href");


            url = URLDecoder.decode(url.substring(url.indexOf('=') + 1, url.indexOf('&')), "UTF-8");
            if (!url.startsWith("http")) {
                continue;
            }
            URLConnection urlCon = new URL(url).openConnection();
            urlCon.setRequestProperty("User-Agent", userAgent);
            urlCon.connect();

            try {
                InputStream is = urlCon.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }
            String redirLink = urlCon.getURL().toString();

            Document doc = Jsoup.connect(redirLink).ignoreContentType(true).get();
            List<Element> list = doc.select("meta[name=description]");

            String description = list.size() == 0 ? doc.title() : list.get(0).attr("content");

            description = description.length() > 512 ? description.substring(0, 512) + "..." : description;
            embed.setDescription(++resultIndex + ") " +
                    "[" + title + "](" + redirLink + ")\n\n**Description:** " + description + "");
            break;
        }

        if (!searchResultIterator.hasNext()) {
            return null;
        }

        Message msg = msgChan.sendMessage(embed.build()).complete();
        if (hasNextOption) {
            msg.addReaction("⏭").queue();
        }
        msgWrapper.setMessage(msg);


        return searchResultList.subList(index, searchResultList.size());
    }

    private void performSearchQuery(Guild guild, MessageChannel msgChan,
                                    Message message, Member member, String search) {
        GoogleResults results = null;
        String google = "http://www.google.com/search?q="; //https://www.youtube.com/results?search_query=test
        String charset = "UTF-8";
        userAgent = "TreeBot";

        try {
            Elements links = Jsoup.connect(google +
                    URLEncoder.encode(search, charset) + "&safe=on")
                    .userAgent(userAgent)
                    .get()
                    .select(".g>.r>a");

            List<Element> elementsList = new ArrayList<>();
            for (Element link : links) {
                elementsList.add(link);
            }



            MessageWrapper msgWrapper = new MessageWrapper();

            // Create the index, and assign the MenuSelectionInfo.
            int index = 0;
            long userId = member.getUser().getIdLong();

            addSelectionEntry(guild, msgChan, userId,
                    new MenuSelectionInfo(null, msgChan, elementsList, null), index);
            List<Element> newList =
                    getNextResult(guild, member, msgChan, msgWrapper, true);

            if (newList == null) {
                return;
            }

            // Create the menu selection info object, add it to the map,
            // and make a reaction menu to be added to the client.
            MenuSelectionInfo msInfo = new MenuSelectionInfo(msgWrapper.getMessage(),
                    msgChan, newList, null);
            addSelectionEntry(guild, msgChan, userId, msInfo, index);
            ReactionMenu menu = new ReactionMenu(commandName, userId, msgChan);

            CommandManager.addReactionMenu(guild,
                    msgWrapper.getMessage().getIdLong(), menu);

        } catch (Exception e) {
            LoggerUtil.logError(e, log, message);
            e.printStackTrace();
            return;
        }
//        msgChan.sendMessage(embed.build()).queue();
    }

    private String getQuery(String[] args) {
        String search = "";
        for (int i = 1; i < args.length; i++) {
            search += args[i] + " ";
        }
        return search.trim();
    }

    @Override
    public void execute(Guild guild, MessageChannel msgChan, Message message, Member member, String[] args) {
        checkIfGuildExists(guild);

        if (args.length < 2) {
            MessageUtil.sendError("No arguments added to the search command.", msgChan);
            return;
        }

        String search = getQuery(args);
        long userId = member.getUser().getIdLong();

        if (hasMenu(guild, userId)) {
            removeSelectionEntry(guild, msgChan, userId);
        }

        performSearchQuery(guild, msgChan, message, member, search);
    }

    @Override
    public String help() {
        return "``" + CommandManager.botToken + commandName +
                " [search]``: Returns the first three search results from a google search query.";
    }

    @Override
    public String getCommandName() {
        return commandName;
    }
}
