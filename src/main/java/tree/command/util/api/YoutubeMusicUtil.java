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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
    private static final String[] AUTHORIZED_ROLES = {"Discord DJ", "Tester", "Moderator"}; //TODO - Read this from file.
    private static MenuUtil menuUtil;

    private YoutubeMusicUtil() {
        youtube = new YouTube.Builder(AuthUtil.HTTP_TRANSPORT, AuthUtil.JSON_FACTORY, new HttpRequestInitializer() {
            public void initialize(HttpRequest request) throws IOException {
            }
        }).setApplicationName("TreeBot").build();
        youtubeAPIKey = Config.getYoutubeAPIKey();
        audioPlayer = AudioPlayerAdapter.audioPlayer;
        menuUtil = MenuUtil.getInstance();
    }

//    public boolean inSameVoiceChannel(Guild guild, Member member) {
//        AudioManager audioManager = guild.getAudioManager();
//        return audioManager.isConnected() &&
//                member.getVoiceState().getChannel().equals(audioManager.getConnectedChannel());
//    }

    public boolean menuIsOpen(Command currCommand, MessageChannel msgChan) {
        if (currCommand instanceof AddCommand) {
            AddCommand addCommand = (AddCommand) currCommand;
            return addCommand.hasMenu() && menuUtil.inSameMessageChannel(msgChan, addCommand.getCommandName());
        } else if (currCommand instanceof RequestCommand) {
            RequestCommand reqCommand = (RequestCommand) currCommand;
            return reqCommand.hasMenu() && menuUtil.inSameMessageChannel(msgChan, reqCommand.getCommandName());
        }
        System.out.println("SHOULD NOT BE REACHED.");
        return false;
    }

    public static YoutubeMusicUtil getInstance() {
        return ytUtil;
    }

    public int youtubeSearch(String query, Guild guild,
                             MessageChannel msgChan, Message message, Member member,
                             String commandName, AtomicInteger atomInt, List<String> songsToChoose,
                             AtomicBoolean waitingForChoice) {
        try {
            atomInt.set(0);
            // This object is used to make YouTube Data API requests. The last
            // argument is required, but since we don't need anything
            // initialized when the HttpRequest is initialized, we override
            // the interface and provide a no-op function.
            YouTube.Search.List search = youtube.search().list("id,snippet");
            initializeSearchFields(search, query);

            // If given a direct URL, get the result and complete the search.
            if (isDirectYoutubeURL(query, msgChan)) {
                return -1;
            }

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
            String messageString = "Type ``" +
                    CommandManager.botToken +
                    commandName +
                    " n`` to select the song, where ``n`` is your choice. \n\n";

            while (iteratorSearchResults.hasNext()) {
                SearchResult singleVideo = iteratorSearchResults.next();
                ResourceId rId = singleVideo.getId();

                // Confirm that the result represents a video. Otherwise, the
                // item will not contain a video ID.
                if (rId.getKind().equals("youtube#video")) {
                    messageString += getMessageString(singleVideo, rId, atomInt, songsToChoose) + "\n";
                }
            }
            Message songListMessage = new MessageBuilder().append(messageString).build();
//            menuUtil.setMenuId(msgChan.getIdLong(), message.getIdLong());
            msgChan.sendMessage(songListMessage).queue(m -> menuUtil.setMenuId(commandName, msgChan, m));
            waitingForChoice.set(true);
            menuUtil.setUserId(commandName, msgChan, member);
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
        for (String roleName : AUTHORIZED_ROLES) {
            List<Role> roles = guild.getRolesByName(roleName, true);
            if (roles.isEmpty()) {
                continue;
            }
            Role authRole = roles.get(0);
            if (member.getRoles().contains(authRole)) {
                return true;
            }
        }
        return false;
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
        search.setMaxResults(4L);
    }

    public void addSong(Guild guild, MessageChannel msgChan,
                         String commandName, Member member, String song) {
        guild.getAudioManager().openAudioConnection(member.getVoiceState().getChannel());
        GuildMusicManager musicManager = AudioPlayerAdapter.audioPlayer.getGuildAudioPlayer(guild);
        musicManager.player.setPaused(false);
        audioPlayer.loadAndPlay(guild.getTextChannelById(msgChan.getIdLong()), song, member);

    }

    private boolean isDirectYoutubeURL(String query, MessageChannel msgChan) {
        // If the user entered a URL, there should be only one selection AND it should only be youtube.
        if (query.contains(".com")) {
            if (query.contains("youtu")) {
                // Just add the first (and only) song given with the URL.
            } else {
                System.out.println(" There aren't any results for your query.");
                MessageUtil.sendError("No results are found", msgChan);
            }
            return true;
        }
        return false;
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
