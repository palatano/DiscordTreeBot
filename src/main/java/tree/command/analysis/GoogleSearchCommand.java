package tree.command.analysis;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import org.apache.http.client.utils.URIBuilder;
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
import java.net.*;
import java.util.*;

import tree.util.LoggerUtil;

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

    private MessageEmbed getSearchResult(List<Element> links,  String search, MessageChannel msgChan, Member member) {
        URI url = null;
//            url = new URIBuilder("http://www.google.com/search").addParameter("q", search).build();
//            Elements links = Jsoup.connect(url.toString())
//                    .userAgent("TreeBot")
//                    .get()
//                    .select(".g");
//            if (links.isEmpty()) {
//                MessageUtil.sendError("No search results found.", msgChan);
//                return null;
//            }

        return null;
    }

    /**
     * In this method, we want to extract the next SearchResult from the list of search results,
     * so that we can send it as an embed.
     *
     * @param guild
     * @param member
     * @param msgChan
     * @param msgWrapper
     * @param hasNextOption
     * @return
     * @throws Exception
     */
    private List<Element> getNextResult(Guild guild, Member member, MessageChannel msgChan,
                                       MessageWrapper msgWrapper, boolean hasNextOption) throws Exception {

        // First, we want to get the userId and get the corresponding info from the guildUserMap,
        // as well as the current resultIndex, or the current search result in the list of search results.
        long userId = member.getUser().getIdLong();
        MenuSelectionInfo msInfo = guildToUserMap.get(guild).get(userId);
        long resultIndex = guildToIndexMap.get(guild).get(userId);
        if (msInfo == null) {
            return null;
        }

        // We want to get the list and then check if its empty, informing the
        // user if so.
        List<Element> searchResultList =
                (List<Element>) msInfo.getListOfChoices();

        if (searchResultList.isEmpty()) {
            System.out.println("No more results found.");
            MessageUtil.sendError("No more results found.", msgChan);
            return null;
        }

        msgChan.sendTyping().queue();

        // add method for getting that next result.
        EmbedBuilder embed = new EmbedBuilder();
        embed.setAuthor("", "https://www.google.com/", "https://www.google.com/favicon.ico");

        // After that, we will iterate down the list, getting the next valid search result/
        List<Element> links = searchResultList;
        int index = 0;
        for (Element link : links) {
            index++;
            Elements list = link.select(".r>a");
            if (list.isEmpty()) {
                continue;
            }
            Element entry = list.first();
            String title = entry.text();
            String resultUrl = entry.absUrl("href").replace(")", "\\)");
            String description = null;
            Elements st = link.select(".st");
            if (!st.isEmpty()) {
                description = st.first().text();
                if (description.isEmpty()) {
                    continue;
                }
            }
            embed.setDescription("**[" + title + "](" + resultUrl + ")**\n" + description);
            break;
        }

        // If the index is equal to the size, then we have no more search results.
        if (index == links.size()) {
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

        URI url = null;
        try {
            url = new URIBuilder("http://www.google.com/search")
                    .addParameter("q", search)
                    .addParameter("safe", "on")
                    .build();
            Elements links = Jsoup.connect(url.toString())
                    .userAgent("TreeBot")
                    .get()
                    .select(".g");
            if (links.isEmpty()) {
                MessageUtil.sendError("No search results found.", msgChan);
                return;
            }

            List<Element> elementsList = new ArrayList<>();
            elementsList.addAll(links);

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
