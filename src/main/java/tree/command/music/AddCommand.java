package tree.command.music;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Thumbnail;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.managers.AudioManager;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tree.Config;
import tree.command.util.AuthUtil;
import tree.command.util.MessageUtil;
import tree.command.util.api.YoutubeMusicUtil;
import tree.command.util.music.AudioPlayerAdapter;
import tree.command.util.music.GuildMusicManager;
import tree.command.util.music.TrackScheduler;
import tree.commandutil.type.MusicCommand;
import tree.util.LoggerUtil;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Valued Customer on 8/12/2017.
 */
public class AddCommand implements MusicCommand {
    private String commandName;
    private YoutubeMusicUtil ytUtil;
    private AudioPlayerAdapter audioPlayer;
    private static Logger logger = LoggerFactory.getLogger(AddCommand.class);
    private static AtomicInteger counter;
    private AtomicBoolean waitingForChoice;
    private static List<String> songsToChoose;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> menuSelectionTask;

    private void createScheduler(Guild guild, MessageChannel msgChan, Message message, Member member) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                ytUtil.getMenuUtil().removeUserId(msgChan.getIdLong());
                waitingForChoice.set(false);
                // If no choice has been selected, pick the first song to add.
                String url = "";
                if (!songsToChoose.isEmpty()) {
                    url = songsToChoose.get(0);
                } else {
                    return;
                }
                ytUtil.addSong(guild, msgChan, message, member, url);
                songsToChoose = new ArrayList<>();
            }
        };

        scheduler = Executors
                .newScheduledThreadPool(1);
//        menuSelectionTask = scheduler.schedule(
//                runnable, 12, TimeUnit.SECONDS);
    }

    public void resetSongsToChoose() {
        songsToChoose = new ArrayList<>();
    }

    public boolean hasMenu() {
        return waitingForChoice.get();
    }

    public ScheduledFuture<?> getMenuSelectionTask() {
        return menuSelectionTask;
    }

    public AtomicBoolean isWaitingForChoice() {
        return waitingForChoice;
    }

    // Race condition. Save the userID who entered the search query first. Let them choose it for 7 seconds
    // If not, reset the search.

    public AddCommand(String commandName) {
        this.commandName = commandName;
        audioPlayer = AudioPlayerAdapter.audioPlayer;
        songsToChoose = new ArrayList<>();
        ytUtil = YoutubeMusicUtil.getInstance();
        counter = new AtomicInteger(0);
        waitingForChoice = new AtomicBoolean(false);
    }


    @Override
    public void execute(Guild guild, MessageChannel msgChan, Message message, Member member, String[] args) {
        if (args.length <= 1) {
            LoggerUtil.logMessage(logger, message, "Only one argument allowed.");
            MessageUtil.sendError("Please provide a song to add.", msgChan);
            return;
        }

        String search = ytUtil.getQuery(args);
        if (!ytUtil.authorizedUser(guild, member)) {
            message.addReaction("\u274E").queue();
            return;
        }

        // Path 1: No query have been entered yet.
        if (!waitingForChoice.get()) {
            if (MessageUtil.checkIfInt(search)) {
                message.addReaction("\u274E").queue();
                return;
            }

            // Cancel the task if its still running./
            if (menuSelectionTask != null && scheduler != null) {
                menuSelectionTask.cancel(true);
            }

            // Return if direct URL search or error has occurred.
            if (ytUtil.youtubeSearch(search, guild, msgChan, message,
                    member, getCommandName(), counter, songsToChoose, waitingForChoice) == -1) {
                return;
            }

            // Create the task again.
            if (menuSelectionTask == null || menuSelectionTask.isCancelled() || menuSelectionTask.isDone()) {
                createScheduler(guild, msgChan, message, member);
                message.addReaction("\u2705").queue();
            }

        } else {
            // Path 2: Not user, ignore command and return.
            long userId = member.getUser().getIdLong();
            long lastUserId = ytUtil.getMenuUtil().getUserId(msgChan.getIdLong());
            if (userId != lastUserId) {
                message.addReaction("\u274E").queue();
                return;
            }
            // Path 3: Not a valid int. Exit query and do not allow searching.
            if (!MessageUtil.checkIfInt(search)) {
                MessageUtil.sendError("Not a numerical response." +
                        " Please search for the video again.", msgChan);
                waitingForChoice.set(false);
                songsToChoose = new ArrayList<>();
                menuSelectionTask.cancel(true);
                return;
            }
            // Path 4: Correct user and valid int. Add song.
            int index = Integer.parseInt(search);
            String url = ytUtil.getSongURL(index, msgChan, songsToChoose);
            ytUtil.addSong(guild, msgChan, message, member, url);
            menuSelectionTask.cancel(true);
            waitingForChoice.set(false);
            message.addReaction("\u2705").queue();
        }
    }

    @Override
    public String help() {
        return "Adds the song with either a keyword search or a direct URL.";
    }

    @Override
    public String getCommandName() {
        return commandName;
    }
}
