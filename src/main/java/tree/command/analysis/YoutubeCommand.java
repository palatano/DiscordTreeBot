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
import tree.command.util.AuthUtil;
import tree.command.util.MessageUtil;
import tree.commandutil.CommandManager;
import tree.commandutil.type.AnalysisCommand;

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
    private static int counter;
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

    private void removeSelectionEntry(Guild guild, MessageChannel msgChan,
                                      Member member) {
        long userId = member.getUser().getIdLong();
        guildToIndexMap.get(guild).remove(userId);
        guildToUserMap.get(guild).remove(userId);
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

    private void youtubeSearch(String query, Guild guild, MessageChannel msgChan, Member member) {
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
            long index = 0;
            MenuSelectionInfo msInfo = new MenuSelectionInfo(null, msgChan, resultList, null);
            addSelectionEntry(guild, msgChan, member, msInfo, index);



//            getNextResult(query, msgChan);

        } catch (GoogleJsonResponseException e) {
            System.err.println("There was a service error: " + e.getDetails().getCode() + " : "
                    + e.getDetails().getMessage());
        } catch (IOException e) {
            System.err.println("There was an IO error: " + e.getCause() + " : " + e.getMessage());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Deprecated
    private EmbedBuilder getMessageEmbed(SearchResult singleVideo, ResourceId rId, EmbedBuilder embed) {
        String result = "";
        String author = singleVideo.getSnippet().getChannelTitle();
        String videoTitle = singleVideo.getSnippet().getTitle();
        String desc = singleVideo.getSnippet().getDescription();

        String beginning = videoTitle;
        String channel = ++counter + ") " + "Channel: " + author;
        String url = "https://www.youtube.com/watch?v=" + rId.getVideoId();
        result += "Video result: " + url;
        embed.setTitle(channel);
        embed.setDescription("[" + beginning + "](" + url + ")");
        embed.appendDescription("\n" + "**Video description: **" + desc);
        Thumbnail thumbnail = singleVideo.getSnippet().getThumbnails().getDefault();
        embed.setThumbnail(thumbnail.getUrl());
        if (counter == 1) {
            embed.addField("Not the video you wanted?", "Type \"" +
                    CommandManager.botToken +
                    getCommandName() +
                    " next\" for more"  +
                    " (Up to two more).", true);
        }
        return embed;
    }

    private String getMessageString(SearchResult singleVideo, ResourceId rId) {
        String result = "";
        String author = singleVideo.getSnippet().getChannelTitle();
        String videoTitle = singleVideo.getSnippet().getTitle();
        String desc = singleVideo.getSnippet().getDescription();

        String beginning = videoTitle;
        String channel = ++counter + ") " + "Channel: " + author;
        String url = "https://www.youtube.com/watch?v=" + rId.getVideoId();
        switch (counter) {
            case 1:
                result += "Video result: (" + url + ")\nNot this one? Type \"" +
                        CommandManager.botToken +
                        getCommandName() +
                        " next\" for more (Up to two more).";
                break;
            case 2:
                result += "Video result: (" + url + ")\nNot this one? Type \"" +
                        CommandManager.botToken +
                        getCommandName() +
                        " next\" for more (Up to one more).";
                break;
            case 3:
                result += "Video result: (" + url + ")";
                break;
            default:
                System.out.println("Should not be reached. Error.");
                break;
        }
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
    private void getNextResult(Guild guild, Member member, MessageChannel msgChan) {
        long userId = member.getUser().getIdLong();
        MenuSelectionInfo msInfo = guildToUserMap.get(guild).get(userId);
        List<SearchResult> searchResultList = (List<SearchResult>) msInfo.getSongsToChoose();

        if (searchResultList.isEmpty()) {
            System.out.println("No more results found.");
            MessageUtil.sendError("No more results found.", msgChan);
            return;
        }

        Iterator<SearchResult> searchResultIterator = searchResultList.iterator();
        while (searchResultIterator.hasNext()) {
            
        }

        EmbedBuilder embed = new EmbedBuilder();
        String messageString = "";






        while (iteratorSearchResults.hasNext()) {

            SearchResult singleVideo = iteratorSearchResults.next();
            ResourceId rId = singleVideo.getId();

            // Confirm that the result represents a video. Otherwise, the
            // item will not contain a video ID.
            if (rId.getKind().equals("youtube#video")) {
                messageString = getMessageString(singleVideo, rId);
                break;
            }
        }
        msgChan.sendMessage(messageString).queue();

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
        } else if (!guildToIndexMap.containsKey(guild)) {
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
        String query = getQuery(args);
        long userId = member.getUser().getIdLong();
        checkIfGuildExists(guild);

        // If the user is currently searching for the next video.
        if (query.equals("next")) {
            if (!guildToUserMap.get(guild).containsKey(userId)) {
                message.addReaction("\u274E").queue();
                return;
            }

            MenuSelectionInfo msInfo = guildToUserMap.get(guild).get(userId);
            long currIndex = guildToIndexMap.get(guild).get(userId);
            long previousMenuId = msInfo.getMenu().getIdLong();
            List<SearchResult> list = (List<SearchResult>) msInfo.getSongsToChoose();



            // getNextResult() { ... }
        } else {
            youtubeSearch(query, guild, msgChan, member);
        }

        // If the user is searching for the first time, or resets the search.


//        if (counter >= 1 && search.equals("next")) {
//            if (counter >= 3) {
//                counter = 0;
//            } else {
//                getNextResult(iteratorSearchResults, origString, msgChan);
//                return;
//            }
//        } else {
//            counter = 0;
//        }

        // If the user

    }

    @Override
    public String help() {
        return "``" + CommandManager.botToken + commandName + " [search] ``: Returns the first three search results from Youtube.";
    }

    @Override
    public String getCommandName() {
        return commandName;
    }
}
