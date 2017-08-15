package tree.command.util.api;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.sun.deploy.config.Config;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import tree.command.util.AuthUtil;
import tree.command.util.MessageUtil;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Valued Customer on 8/15/2017.
 */
public class YoutubeUtil {

//    public static void initializeSearchFields(YouTube.Search.List search, String query) {
//        // Define the API request for retrieving search results.
//
//        String apiKey = null;
//        search.setKey(apiKey);
//        search.setQ(query);
//        search.setSafeSearch("moderate");
//        search.setType("video");
//
//        // To increase efficiency, only retrieve the fields that the
//        // application uses.
//        search.setFields("items(snippet/channelTitle,id/kind,id/videoId,snippet/title," +
//                "snippet/thumbnails/default/url,snippet/description)");
//        search.setMaxResults(4L);
//    }
//
//    public static int youtubeSearch(String query, Guild guild, MessageChannel msgChan, Message message, Member member) {
//
//        try {
//            counter = 0;
//            // This object is used to make YouTube Data API requests. The last
//            // argument is required, but since we don't need anything
//            // initialized when the HttpRequest is initialized, we override
//            // the interface and provide a no-op function.
//            youtube = new YouTube.Builder(AuthUtil.HTTP_TRANSPORT, AuthUtil.JSON_FACTORY, new HttpRequestInitializer() {
//                public void initialize(HttpRequest request) throws IOException {
//                }
//            }).setApplicationName("TreeBot").build();
//
//            YouTube.Search.List search = youtube.search().list("id,snippet");
//            initializeSearchFields(search, query);
//
//            // Fetch the search results.
//            SearchListResponse searchResponse = search.execute();
//            List<SearchResult> resultList = searchResponse.getItems();
//            if (resultList.isEmpty() || resultList == null) {
//                System.out.println(" There aren't any results for your query.");
//                MessageUtil.sendError("No results are found", msgChan);
//                return -1;
//            }
//
//            // If the user entered a URL, there should be only one selection AND it should only be youtube.
//            if (query.contains(".com")) {
//                if (query.contains("youtu")) {
//                    // Just add the first (and only) song given with the URL.
//                    addSong(guild, msgChan, message, member, query);
//                } else {
//                    System.out.println(" There aren't any results for your query.");
//                    MessageUtil.sendError("No results are found", msgChan);
//                }
//                return -1;
//            }
//
//            // Create an iterator for the results, and then merge into a list.
//            Iterator<SearchResult> iteratorSearchResults = resultList.iterator();
//            EmbedBuilder embed = new EmbedBuilder();
//            String messageString = "Type ``;add n`` to select the song, where ``n`` is your choice. \n\n";
//
//            while (iteratorSearchResults.hasNext()) {
//                SearchResult singleVideo = iteratorSearchResults.next();
//                ResourceId rId = singleVideo.getId();
//
//                // Confirm that the result represents a video. Otherwise, the
//                // item will not contain a video ID.
//                if (rId.getKind().equals("youtube#video")) {
//                    messageString += getMessageString(singleVideo, rId) + "\n";
//                }
//            }
//            Message songListMessage = new MessageBuilder().append(messageString).build();
//            menuMessageGuildMap.put(guild.getIdLong(), message.getIdLong());
//            msgChan.sendMessage(songListMessage).queue(m -> menuMessageGuildMap.put(guild.getIdLong(), m.getIdLong()));
//            waitingForChoice = true;
//            userSelecting = member.getUser().getIdLong();
//        } catch (GoogleJsonResponseException e) {
//            System.err.println("There was a service error: " + e.getDetails().getCode() + " : "
//                    + e.getDetails().getMessage());
//        } catch (IOException e) {
//            System.err.println("There was an IO error: " + e.getCause() + " : " + e.getMessage());
//        } catch (Throwable t) {
//            t.printStackTrace();
//        }
//        return 0;
//    }
}
