package tree.command.analysis;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Thumbnail;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import tree.Config;
import tree.command.data.GoogleResults;
import tree.command.data.MenuSelectionInfo;
import tree.command.data.MessageWrapper;
import tree.command.data.ReactionMenu;
import tree.command.util.AuthUtil;
import tree.command.util.MessageUtil;
import tree.commandutil.CommandManager;
import tree.commandutil.type.AnalysisCommand;
import tree.commandutil.util.CommandRegistry;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by Valued Customer on 8/3/2017.
 */
public class YoutubeCommand implements AnalysisCommand {
    private String commandName;
    private String userAgent;
    private YouTube youtube;
    private Map<Guild, Map<Long, MenuSelectionInfo>> guildToUserMap;
    private Map<Guild, Map<Long, Long>> guildToIndexMap;
    private Iterator<SearchResult> iteratorSearchResults;
    private String origString;
    private String youtubeAPIKey;
    private static final int MAX_RESULTS = 3;

    public void initializeSearchFields(YouTube.Search.List search, String query) {
        // Define the API request for retrieving search results.

        search.setKey(youtubeAPIKey);
        search.setQ(query);
        search.setSafeSearch("moderate");
        search.setType("video");

        // To increase efficiency, only retrieve the fields that the
        // application uses.
        search.setFields("items(snippet/channelTitle,id/kind,id/videoId,snippet/title," +
                "snippet/thumbnails/default/url,snippet/description)");
        search.setMaxResults((long) MAX_RESULTS);
    }

    private void addSelectionEntry(Guild guild, MessageChannel msgChan,
                                   Member member, MenuSelectionInfo msInfo, long index) {
        guildToUserMap.get(guild).put(member.getUser().getIdLong(), msInfo);
        guildToIndexMap.get(guild).put(member.getUser().getIdLong(), index);
    }

    private MenuSelectionInfo removeSelectionEntry(Guild guild, MessageChannel msgChan,
                                      Member member) {
        long userId = member.getUser().getIdLong();
        Map<Long, MenuSelectionInfo> userSelectionMap = guildToUserMap.get(guild);
        if (!userSelectionMap.containsKey(userId)) {
            System.out.println("Bot is attempting to remove a non-existent user.");
            return null;
        }

        long index = 0;
        if (guildToIndexMap.get(guild).containsKey(userId)) {
            index = guildToIndexMap.get(guild).remove(userId);
        } else {
            System.out.println("Trying to remove a non-existent user.");
        }

        MenuSelectionInfo msInfo = userSelectionMap.remove(userId);
        long menuId = msInfo.getMenu().getIdLong();
        MessageChannel messageChannel = msInfo.getChannel();
        if (menuId != 0) {
            messageChannel.deleteMessageById(menuId).queue();
        }
        CommandManager.removeReactionMenu(guild, menuId);
        return msInfo;
    }

    public boolean isSelectingUser(Guild guild, Member member) {
        return guildToUserMap.get(guild).containsKey(member.getUser().getIdLong());
    }

    public void nextOption(Guild guild, MessageChannel msgChan, Member member, long menuId) {
        long userId = member.getUser().getIdLong();
        Map<Long, MenuSelectionInfo> menuSelectionInfoMap = guildToUserMap.get(guild);
        Map<Long, Long> indexMap = guildToIndexMap.get(guild);
        long index = indexMap.get(userId);
        MenuSelectionInfo prevMsInfo = menuSelectionInfoMap.get(userId);

        boolean hasNextOption = true;
        if (++index >= MAX_RESULTS) {
            hasNextOption = false;
        }
        guildToIndexMap.get(guild).put(userId, index);

        MessageWrapper msgWrapper = new MessageWrapper();
        List<SearchResult> newList = getNextResult(guild, member, msgChan, msgWrapper, hasNextOption);
        if (newList == null) {
            return;
        }

        // Delete the previous menu.
        if (hasMenu(guild, userId)) {
            removeSelectionEntry(guild, msgChan, member);
        }

            MenuSelectionInfo msInfo =
                    new MenuSelectionInfo(msgWrapper.getMessage(), msgChan,
                            newList, null);
            addSelectionEntry(guild, msgChan, member, msInfo, index);
            ReactionMenu menu = new ReactionMenu(commandName, userId, msgChan);
            CommandManager.addReactionMenu(guild,
                    msgWrapper.getMessage().getIdLong(), menu);

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

    public YoutubeCommand(String commandName) {
        this.commandName = commandName;
        youtubeAPIKey = Config.getYoutubeAPIKey();
        guildToUserMap = new HashMap<>();
        guildToIndexMap = new HashMap<>();

        // This object is used to make YouTube Data API requests. The last
        // argument is required, but since we don't need anything
        // initialized when the HttpRequest is initialized, we override
        // the interface and provide a no-op function.
        youtube = new YouTube.Builder(AuthUtil.HTTP_TRANSPORT, AuthUtil.JSON_FACTORY, new HttpRequestInitializer() {
            public void initialize(HttpRequest request) throws IOException {
            }
        }).setApplicationName("TreeBot").build();
    }

    private void youtubeSearch(String query, Guild guild,
                               MessageChannel msgChan, Member member) {
        try {
            // Define the API request for retrieving search results.
            YouTube.Search.List search = youtube.search().list("id,snippet");
            origString = query;

            initializeSearchFields(search, query);

            SearchListResponse searchResponse = search.execute();
            List<SearchResult> resultList = searchResponse.getItems();
            if (resultList.isEmpty() || resultList == null) {
                System.out.println(" There aren't any results for your query.");
                MessageUtil.sendError("No results were found.", msgChan);
                return;
            }

            // Create the index, and assign the MenuSelectionInfo.
            long index = 1;
            long userId = member.getUser().getIdLong();

            MessageWrapper msgWrapper = new MessageWrapper();

            addSelectionEntry(guild, msgChan, member,
                    new MenuSelectionInfo(null, msgChan, resultList, null), index);
            List<SearchResult> newList =
                    getNextResult(guild, member, msgChan, msgWrapper, true);
            if (newList == null) {
                return;
            }

            MenuSelectionInfo msInfo = new MenuSelectionInfo(msgWrapper.getMessage(),
                    msgChan, newList, null);
            addSelectionEntry(guild, msgChan, member, msInfo, index);
            ReactionMenu menu = new ReactionMenu(commandName, userId, msgChan);
            CommandManager.addReactionMenu(guild,
                    msgWrapper.getMessage().getIdLong(), menu);

        } catch (GoogleJsonResponseException e) {
            System.err.println("There was a service error: " + e.getDetails().getCode() + " : "
                    + e.getDetails().getMessage());
        } catch (IOException e) {
            System.err.println("There was an IO error: " + e.getCause() + " : " + e.getMessage());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private String getMessageString(SearchResult singleVideo, ResourceId rId) {
        String result = "";
        String author = singleVideo.getSnippet().getChannelTitle();
        String videoTitle = singleVideo.getSnippet().getTitle();
        String desc = singleVideo.getSnippet().getDescription();

        String beginning = videoTitle;
//        String channel = ++counter + ") " + "Channel: " + author;
        String url = "https://www.youtube.com/watch?v=" + rId.getVideoId();
        result += "Video result: (" + url + ")";
        return result;

    }



    /*
 * Prints out all results in the Iterator. For each result, print the
 * title, video ID, and thumbnail.
 *
 * @param iteratorSearchResults Iterator of SearchResults to print
 *
 * @param query Search query (String)
 */
    private List<SearchResult> getNextResult(Guild guild, Member member,
                               MessageChannel msgChan, MessageWrapper msgWrapper,
                               boolean hasNextOption) {
        long userId = member.getUser().getIdLong();
        MenuSelectionInfo msInfo = guildToUserMap.get(guild).get(userId);
        long resultIndex = guildToIndexMap.get(guild).get(userId);
        if (msInfo == null) {
            return null;
        }

        List<SearchResult> searchResultList =
                (List<SearchResult>) msInfo.getSongsToChoose();

        if (searchResultList.isEmpty()) {
            System.out.println("No more results found.");
            MessageUtil.sendError("No more results found.", msgChan);
            return null;
        }

        EmbedBuilder embed = new EmbedBuilder();
        String messageString = "";
        if (resultIndex == 1) {
            messageString += "First ";
        } else if (resultIndex == 2) {
            messageString += "Second ";
        } else if (resultIndex == 3) {
            messageString += "Last ";
        }

        Iterator<SearchResult> searchResultIterator = searchResultList.iterator();
        int index = 0;
        while (searchResultIterator.hasNext()) {
            index++;
            SearchResult singleVideo = searchResultIterator.next();
            ResourceId rId = singleVideo.getId();

            // Confirm that the result represents a video. Otherwise, the
            // item will not contain a video ID.
            if (rId.getKind().equals("youtube#video")) {
                messageString += getMessageString(singleVideo, rId);
                break;
            }
        }
        Message msg = msgChan.sendMessage(messageString).complete();
        if (hasNextOption) {
            msg.addReaction("⏭").queue();
        }
        msgWrapper.setMessage(msg);

        return searchResultList.subList(index, searchResultList.size());
    }

    private String getQuery(String[] args) {
        String search = "";
        for (int i = 1; i < args.length; i++) {
            search += args[i] + " ";
        }
        return search.trim();
    }

    private void checkIfGuildExists(Guild guild) {
        if (!guildToUserMap.containsKey(guild)) {
            guildToUserMap.put(guild, new HashMap<>());
        }
        if (!guildToIndexMap.containsKey(guild)) {
            guildToIndexMap.put(guild, new HashMap<>());
        }
    }

    @Override
    public void execute(Guild guild, MessageChannel msgChan,
                        Message message, Member member, String[] args) {

        if (args.length < 2) {
            MessageUtil.sendError("No arguments added to the search command.", msgChan);
            return;
        }
        checkIfGuildExists(guild);

        String query = getQuery(args);
        long userId = member.getUser().getIdLong();
        if (hasMenu(guild, userId)) {
            removeSelectionEntry(guild, msgChan, member);
        }

        youtubeSearch(query, guild, msgChan, member);
    }

    @Override
    public String help() {
        return "``" + CommandManager.botToken + commandName +
                " [search] ``: Returns the first three search results from Youtube.";
    }

    @Override
    public String getCommandName() {
        return commandName;
    }
}
