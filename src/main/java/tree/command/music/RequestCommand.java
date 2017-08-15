package tree.command.music;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.*;
import org.apache.http.auth.AUTH;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tree.Config;
import tree.command.util.AuthUtil;
import tree.command.util.MessageUtil;
import tree.command.util.music.AudioPlayerAdapter;
import tree.command.util.music.GuildMusicManager;
import tree.commandutil.type.MusicCommand;
import tree.util.LoggerUtil;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by Valued Customer on 8/15/2017.
 */
public class RequestCommand implements MusicCommand {
    private String commandName;
    private AudioPlayerAdapter audioPlayer;
    private static Logger logger = LoggerFactory.getLogger(AddCommand.class);
    private YouTube youtube;
    private static int counter;
    private boolean waitingForChoice = false;
    private static List<String> songsToChoose;
    private String youtubeAPIKey;
    private ScheduledExecutorService scheduler;
    private Map<Long, Long> menuMessageGuildMap;
    private ScheduledFuture<?> menuSelectionTask;
    private static final String[] AUTHORIZED_ROLES = {"Discord DJ", "Tester", "Moderator"};

    private void createScheduler(Guild guild, MessageChannel msgChan, Message message, Member member) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
//                userSelecting = 0L;
                waitingForChoice = false;
                songsToChoose = new ArrayList<>();
                // Delete the menu.
                if (menuMessageGuildMap.containsKey(guild.getIdLong())) {
                    long menuId = menuMessageGuildMap.get(guild.getIdLong());
                    msgChan.deleteMessageById(menuId).queue();
                }
            }
        };


//        scheduledPool = Executors.newScheduledThreadPool(3);
        menuSelectionTask = scheduler.schedule(
                runnable, 25, TimeUnit.SECONDS);
//        scheduledPool.schedule(runnable,  12, TimeUnit.SECONDS);
    }

    private boolean authorizedUser(Guild guild, Member member) {
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

    public void cancelMenu(Guild guild, MessageChannel msgChan) {
        waitingForChoice = false;
        // If no choice has been selected, pick the first song to add.
        songsToChoose = new ArrayList<>();
        if (!menuSelectionTask.isCancelled()) {
            menuSelectionTask.cancel(true);
        }
        if (!menuMessageGuildMap.containsKey(guild.getIdLong())) {
            return;
        }
        long messageId = menuMessageGuildMap.get(guild.getIdLong());
        msgChan.deleteMessageById(messageId).queue();
    }

    public boolean hasMenu() {
        return waitingForChoice;
    }

    // Race condition. Save the userID who entered the search query first. Let them choose it for 7 seconds
    // If not, reset the search.

    public RequestCommand(String commandName) {
        this.commandName = commandName;
        youtubeAPIKey = Config.getYoutubeAPIKey();
        audioPlayer = AudioPlayerAdapter.audioPlayer;
        songsToChoose = new ArrayList<>();
        menuMessageGuildMap = new HashMap<>();
        scheduler = Executors
                .newScheduledThreadPool(1);
    }

    private void initializeSearchFields(YouTube.Search.List search, String query) {
        // Define the API request for retrieving search results.

        String apiKey = youtubeAPIKey;
        search.setKey(apiKey);
        search.setQ(query);
        search.setSafeSearch("moderate");
        search.setType("video");

        // To increase efficiency, only retrieve the fields that the
        // application uses.
        search.setFields("items(snippet/channelTitle,id/kind,id/videoId,snippet/title," +
                "snippet/thumbnails/default/url,snippet/description)");
        search.setMaxResults(4L);

    }

    private int youtubeSearch(String query, Guild guild, MessageChannel msgChan, Message message, Member member) {

        try {
            counter = 0;
            // This object is used to make YouTube Data API requests. The last
            // argument is required, but since we don't need anything
            // initialized when the HttpRequest is initialized, we override
            // the interface and provide a no-op function.
            youtube = new YouTube.Builder(AuthUtil.HTTP_TRANSPORT, AuthUtil.JSON_FACTORY, new HttpRequestInitializer() {
                public void initialize(HttpRequest request) throws IOException {
                }
            }).setApplicationName("TreeBot").build();

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

            // If the user entered a URL, there should be only one selection AND it should only be youtube.
            if (query.contains(".com")) {
                if (query.contains("youtu")) {
                    if (!authorizedUser(guild, member)) {
                        message.addReaction("\u274E").queue();
                        return -1;
                    }
                    // Just add the first (and only) song given with the URL.
                    addSong(guild, msgChan, message, member, query);
                    message.addReaction("\u2705").queue();
                } else {
                    System.out.println(" There aren't any results for your query.");
                    MessageUtil.sendError("No results are found", msgChan);
                    message.addReaction("\u274E").queue();
                }
                return -1;
            }

            // Create an iterator for the results, and then merge into a list.
            Iterator<SearchResult> iteratorSearchResults = resultList.iterator();
            EmbedBuilder embed = new EmbedBuilder();
            String messageString = "Authorized users, type ``;req n`` to select the song, where ``n`` is your choice. \n\n";

            while (iteratorSearchResults.hasNext()) {
                SearchResult singleVideo = iteratorSearchResults.next();
                ResourceId rId = singleVideo.getId();

                // Confirm that the result represents a video. Otherwise, the
                // item will not contain a video ID.
                if (rId.getKind().equals("youtube#video")) {
                    messageString += getMessageString(singleVideo, rId) + "\n";
                }
            }
            Message songListMessage = new MessageBuilder().append(messageString).build();
            menuMessageGuildMap.put(guild.getIdLong(), message.getIdLong());
            msgChan.sendMessage(songListMessage).queue(m -> {
                        menuMessageGuildMap.put(guild.getIdLong(), m.getIdLong());
                    }
            );
            waitingForChoice = true;

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


    private static String getMessageString(SearchResult singleVideo, ResourceId rId) {
        String result = "";
        String author = singleVideo.getSnippet().getChannelTitle();
        String videoTitle = singleVideo.getSnippet().getTitle();
        String desc = singleVideo.getSnippet().getDescription();

        String beginning = ++counter + ") " + "``" + videoTitle + "`` ";
        String channel = "from channel ``" + author + "``";
        result += beginning + channel ;
        String url = "https://www.youtube.com/watch?v=" + rId.getVideoId();
        songsToChoose.add(url);
        return result;

    }

    private void addSong(Guild guild, MessageChannel msgChan, Message message, Member member, String song) {
        GuildMusicManager musicManager = AudioPlayerAdapter.audioPlayer.getGuildAudioPlayer(guild);
        musicManager.player.setPaused(false);
        audioPlayer.loadAndPlay(message.getTextChannel(), song, member);
        long messageId = menuMessageGuildMap.get(guild.getIdLong());
        msgChan.deleteMessageById(messageId).queue();
        menuMessageGuildMap.remove(guild.getIdLong());
        waitingForChoice = false;
        message.addReaction("\u2705").queue();
    }

    private String getSongURL(int i, MessageChannel msgChan) {
        if (i < 1 || i >= songsToChoose.size() + 1) {
            MessageUtil.sendError("Enter a valid number from the list.", msgChan);
            return null;
        }
        String url = songsToChoose.get(i - 1);
        songsToChoose = new ArrayList<>();
        return url;
    }

    @Override
    public void execute(Guild guild, MessageChannel msgChan, Message message, Member member, String[] args) {
        if (args.length <= 1) {
            LoggerUtil.logMessage(logger, message, "Only one argument allowed.");
            MessageUtil.sendError("Please provide a song to add.", msgChan);
            return;
        }
        String search = "";
        for (int i = 1; i < args.length; i++) {
            search += args[i] + " ";
        }
        search = search.trim();
        // Path 1: No query have been entered yet. ALLOW a non-authorized
        // user to request and bring up the selection menu. Do not allow them to
        // add though.
        if (!waitingForChoice) {
            if (MessageUtil.checkIfInt(search)) {
                return;
            }
            if (menuSelectionTask != null && scheduler != null) {
                menuSelectionTask.cancel(true);
            }
            if (youtubeSearch(search, guild, msgChan, message, member) == -1) {
                return;
            }
            if (menuSelectionTask == null || menuSelectionTask.isCancelled() || menuSelectionTask.isDone()) {
                createScheduler(guild, msgChan, message, member);
                message.addReaction("\u2705").queue();
            }
        } else {
            // Path 2: Not user, ignore command and return.
            if (!authorizedUser(guild, member)) {
                message.addReaction("\u274E").queue();
                return;
            }
            // Path 3: Not a valid int. Exit query and do not allow searching.
            if (!MessageUtil.checkIfInt(search)) {
                MessageUtil.sendError("Not a numerical response." +
                        " Please search for the video again.", msgChan);
                waitingForChoice = false;
                songsToChoose = new ArrayList<>();
                menuSelectionTask.cancel(true);
                return;
            }
            // Path 4: Correct user and valid int. Add song.
            int index = Integer.parseInt(search);
            String url = getSongURL(index, msgChan);
            addSong(guild, msgChan, message, member, url);
            menuSelectionTask.cancel(true);
        }
    }

    @Override
    public String help() {
        return "Allow a non-authorized user to request a song and an authorized user to add it.";
    }

    @Override
    public String getCommandName() {
        return commandName;
    }
}
