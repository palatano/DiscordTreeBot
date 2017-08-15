package tree.command.analysis;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.services.youtube.*;
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
import tree.command.util.AuthUtil;
import tree.command.util.MessageUtil;
import tree.commandutil.type.AnalysisCommand;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Valued Customer on 8/3/2017.
 */
public class YoutubeCommand implements AnalysisCommand {
    private String commandName;
    private String userAgent;
    private static int counter;
    private YouTube youtube;
    private Iterator<SearchResult> iteratorSearchResults;
    private String origString;
    private String youtubeAPIKey;

    public YoutubeCommand(String commandName) {
        this.commandName = commandName;
        youtubeAPIKey = Config.getYoutubeAPIKey();
    }

    private void youtubeSearch(String query, MessageChannel msgChan) {
        try {

            // This object is used to make YouTube Data API requests. The last
            // argument is required, but since we don't need anything
            // initialized when the HttpRequest is initialized, we override
            // the interface and provide a no-op function.
            youtube = new YouTube.Builder(AuthUtil.HTTP_TRANSPORT, AuthUtil.JSON_FACTORY, new HttpRequestInitializer() {
                public void initialize(HttpRequest request) throws IOException {
                }
            }).setApplicationName("TreeBot").build();

            // Define the API request for retrieving search results.
            YouTube.Search.List search = youtube.search().list("id,snippet");
            origString = query;

            String apiKey = youtubeAPIKey;
            search.setKey(apiKey);
            search.setQ(query);
            search.setSafeSearch("moderate");
            search.setType("video");

            // To increase efficiency, only retrieve the fields that the
            // application uses.
            search.setFields("items(snippet/channelTitle,id/kind,id/videoId,snippet/title," +
                    "snippet/thumbnails/default/url,snippet/description)");
            search.setMaxResults(3L);

            SearchListResponse searchResponse = search.execute();
            List<SearchResult> resultList = searchResponse.getItems();
            if (resultList.isEmpty() || resultList == null) {
                System.out.println(" There aren't any results for your query.");
                MessageUtil.sendError("No results are sent", msgChan);
            }

            iteratorSearchResults = resultList.iterator();
            getNextResult(iteratorSearchResults, query, msgChan);
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
    private static EmbedBuilder getMessageEmbed(SearchResult singleVideo, ResourceId rId, EmbedBuilder embed) {
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
            embed.addField("Not the video you wanted?", "Type \"&youtube next\" for more"  +
                    " (Up to two more).", true);
        }
        return embed;
    }

    private static String getMessageString(SearchResult singleVideo, ResourceId rId) {
        String result = "";
        String author = singleVideo.getSnippet().getChannelTitle();
        String videoTitle = singleVideo.getSnippet().getTitle();
        String desc = singleVideo.getSnippet().getDescription();

        String beginning = videoTitle;
        String channel = ++counter + ") " + "Channel: " + author;
        String url = "https://www.youtube.com/watch?v=" + rId.getVideoId();
        switch (counter) {
            case 1:
                result += "Video result: (" + url + ")\nNot this one? Type \"&youtube next\" for more"  +
                        " (Up to two more).";
                break;
            case 2:
                result += "Video result: (" + url + ")\nNot this one? Type \"&youtube next\" for more"  +
                        " (Up to one more).";
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
    private static void getNextResult(Iterator<SearchResult> iteratorSearchResults,
                                    String query, MessageChannel msgChan) {
        EmbedBuilder embed = new EmbedBuilder();
        String messageString = "";

        if (!iteratorSearchResults.hasNext()) {
            System.out.println("No more results found.");
            MessageUtil.sendError("No more results found.", msgChan);
        }

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

    @Override
    public void execute(Guild guild, MessageChannel msgChan,
                        Message message, Member member, String[] args) {
        String search = "";
        if (args.length < 2) {
            MessageUtil.sendError("No arguments added to the search command.", msgChan);
            return;
        }
        for (int i = 1; i < args.length; i++) {
            search += args[i] + " ";
        }
        search = search.trim();
        if (counter >= 1 && search.equals("next")) {
            if (counter >= 3) {
                counter = 0;
            } else {
                getNextResult(iteratorSearchResults, origString, msgChan);
                return;
            }
        } else {
            counter = 0;
        }
        youtubeSearch(search.trim(), msgChan);
    }

    @Override
    public String help() {
        return "Returns the first three search results from Youtube.";
    }

    @Override
    public String getCommandName() {
        return commandName;
    }
}
