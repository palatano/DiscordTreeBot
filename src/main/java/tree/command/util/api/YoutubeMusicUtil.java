package tree.command.util.api;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.sun.org.apache.regexp.internal.RE;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.managers.AudioManager;
import tree.command.data.MessageWrapper;
import tree.command.music.AddCommand;
import tree.command.music.RequestCommand;
import tree.command.util.AuthUtil;
import tree.command.util.MenuUtil;
import tree.command.util.MessageUtil;
import tree.command.util.music.AudioPlayerAdapter;
import tree.command.util.music.GuildMusicManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import tree.Config;
import tree.commandutil.CommandManager;
import tree.commandutil.type.Command;
import tree.commandutil.util.CommandRegistry;

/**
 * Created by Valued Customer on 8/15/2017.
 */
public class YoutubeMusicUtil {
    private static YoutubeMusicUtil ytUtil = new YoutubeMusicUtil();
    private AudioPlayerAdapter audioPlayer;
    private YouTube youtube;
    private String youtubeAPIKey;
    private static final String[] AUTHORIZED_ROLES = {"Discord DJ", "Moderator"}; //TODO - Read this from file.
    private static MenuUtil menuUtil;
    public static final int MAX_RESULTS = 3;

    private YoutubeMusicUtil() {
        youtube = new YouTube.Builder(AuthUtil.HTTP_TRANSPORT, AuthUtil.JSON_FACTORY, new HttpRequestInitializer() {
            public void initialize(HttpRequest request) throws IOException {
            }
        }).setApplicationName("TreeBot").build();
        youtubeAPIKey = Config.getYoutubeAPIKey();
        audioPlayer = AudioPlayerAdapter.audioPlayerAdapter;
        menuUtil = MenuUtil.getInstance();
    }

    public static YoutubeMusicUtil getInstance() {
        return ytUtil;
    }

    public int youtubeSearch(String query, Guild guild,
                             MessageChannel msgChan, MessageWrapper menuWrapper, Member member,
                             String commandName, AtomicInteger atomInt, List<String> songsToChoose) {
        try {
            // If given a direct URL, get the result and complete the search.
            if (isDirectYoutubeURL(query, msgChan) == 1) {
                // Add the song.
                if (commandName.equals("add")) {
                    addSong(guild, msgChan, commandName, member, query);
                    return -1;
                } else {
                    String directString = "**Authorized Users**: Confirm the direct URL link by typing ``" +
                            CommandManager.botToken +
                            commandName +
                            " 1``.";
                    songsToChoose.add(query);
                    menuWrapper.setMessage(msgChan.sendMessage(directString).complete());
                    return 0;
                }
            } else if (isDirectYoutubeURL(query, msgChan) == 2) {
                return -1;
            }

            // This object is used to make YouTube Data API requests. The last
            // argument is required, but since we don't need anything
            // initialized when the HttpRequest is initialized, we override
            // the interface and provide a no-op function.
            YouTube.Search.List search = youtube.search().list("id,snippet");
            initializeSearchFields(search, query);

            // Fetch the search results.
            SearchListResponse searchResponse = search.execute();
            List<SearchResult> resultList = searchResponse.getItems();
            if (resultList.isEmpty() || resultList == null) {
                System.out.println(" There aren't any results for your query.");
                MessageUtil.sendError("No results are found", msgChan);
                return -1;
            }

            // Create an iterator for the results, and then merge into a list.
            Iterator<SearchResult> iteratorSearchResults = resultList.iterator();
            String messageString = "";
            if (commandName.equals("add")) {
                messageString += "**" + member.getEffectiveName();
            } else if (commandName.equals("req")) {
                messageString += "**Authorized Users";
            }
            messageString += "**: Choose your song:\n\n";

            while (iteratorSearchResults.hasNext()) {
                SearchResult singleVideo = iteratorSearchResults.next();
                ResourceId rId = singleVideo.getId();

                // Confirm that the result represents a video. Otherwise, the
                // item will not contain a video ID.
                if (rId.getKind().equals("youtube#video")) {
                    messageString += getMessageString(singleVideo, rId, atomInt, songsToChoose) + "\n";
                }
            }
            menuWrapper.setMessage(msgChan.sendMessage(messageString).complete());
        } catch (GoogleJsonResponseException e) {
            System.err.println("There was a service error: " + e.getDetails().getCode() + " : "
                    + e.getDetails().getMessage());
        } catch (IOException e) {
            System.err.println("There was an IO error: " + e.getCause() + " : " + e.getMessage());
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return 0;
    }

    public String getQuery(String[] args) {
        String search = "";
        for (int i = 1; i < args.length; i++) {
            search += args[i] + " ";
        }
        return search.trim();
    }

    public boolean authorizedUser(Guild guild, Member member) {
        return Config.hasMusicRole(guild, member);
    }

    public String getSongURL(int i, MessageChannel msgChan, List<String> songsToChoose) {
        if (i < 1 || i >= songsToChoose.size() + 1) {
            MessageUtil.sendError("Enter a valid number from the list.", msgChan);
            return null;
        }
        String url = songsToChoose.get(i - 1);
        return url;
    }

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

    public void addSong(Guild guild, MessageChannel msgChan,
                         String commandName, Member member, String song) {
        AudioManager audioManager = guild.getAudioManager();
        if (!audioManager.isConnected() || !audioManager.isAttemptingToConnect()) {
            guild.getAudioManager().openAudioConnection(member.getVoiceState().getChannel());
        }
        GuildMusicManager musicManager = AudioPlayerAdapter.audioPlayerAdapter
                .getGuildAudioPlayer(guild);
        musicManager.player.setPaused(false);
        audioPlayer.loadAndPlay(guild.getTextChannelById(msgChan.getIdLong()), song, member);

    }

    /**
     *
     * @param query
     * @param msgChan
     * @return 0 - Not a URL.
     *         1 - Is a youtube URL
     *         2 - Not a youtube URL.
     */
    private int isDirectYoutubeURL(String query, MessageChannel msgChan) {
        // If the user entered a URL, there should be only one selection AND it should only be youtube.
        if (query.contains("youtu.be") || query.contains(".com")) {
            if (query.contains("youtu")) {
                return 1;
            } else {
                System.out.println("Youtube links are only supported.");
                MessageUtil.sendError("Youtube links are only supported.", msgChan);
                return 2;
            }
        }
        return 0;
    }

    private static String getMessageString(SearchResult singleVideo, ResourceId rId, AtomicInteger atomInt,
                                           List<String> songsToChoose) {
        String result = "";
        String author = singleVideo.getSnippet().getChannelTitle();
        String videoTitle = singleVideo.getSnippet().getTitle();
        String desc = singleVideo.getSnippet().getDescription();

        String beginning = atomInt.addAndGet(1) + ") " + "``" + videoTitle + "`` ";
        String channel = "from channel ``" + author + "``";
        result += beginning + channel ;
        String url = "https://www.youtube.com/watch?v=" + rId.getVideoId();
        songsToChoose.add(url);
        return result;

    }

    public MenuUtil getMenuUtil() {
        return menuUtil;
    }


}
