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
import tree.commandutil.util.CommandRegistry;
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
    private static Logger logger = LoggerFactory.getLogger(AddCommand.class);
    private static AtomicInteger counter;
    private AtomicBoolean waitingForChoice;
    private static List<String> songsToChoose;
    private ScheduledFuture<?> menuSelectionTask;

    private Runnable createRunnable(Guild guild, MessageChannel msgChan, Member member) {
        return () -> {
                // If no choice has been selected, pick the first song to add.
                if (!songsToChoose.isEmpty()) {
                    String url = songsToChoose.get(0);
                    ytUtil.addSong(guild, msgChan, commandName, member, url);
                }
                reset(msgChan);
                ytUtil.getMenuUtil().deleteMenu(msgChan, commandName);
        };
    }

    public void reset(MessageChannel msgChan) {
        songsToChoose = new ArrayList<>();
        waitingForChoice.set(false);
        if (menuSelectionTask.isCancelled() || menuSelectionTask.isDone()) {
            menuSelectionTask.cancel(true);
        }
    }

    public boolean hasMenu() {
        return waitingForChoice.get();
    }

    // Race condition. Save the userID who entered the search query first. Let them choose it for 7 seconds
    // If not, reset the search.

    public AddCommand(String commandName) {
        this.commandName = commandName;
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

        if (!member.getVoiceState().inVoiceChannel()) {
            MessageUtil.sendError("You must be in a voice channel.", msgChan);
            return;
        }

        String search = ytUtil.getQuery(args);
        if (!ytUtil.authorizedUser(guild, member)) { // || ytUtil.menuIsOpen(this, msgChan)
            message.addReaction("\u274E").queue();
            return;
        }

        // Path 1: No query have been entered yet.
        if (!waitingForChoice.get()) {
            if (MessageUtil.checkIfInt(search)) {
                message.addReaction("\u274E").queue();
                return;
            }

            // Return if direct URL search or error has occurred.
            if (ytUtil.youtubeSearch(search, guild, msgChan, message,
                    member, getCommandName(), counter, songsToChoose, waitingForChoice) == -1) {
                return;
            }

            menuSelectionTask = ytUtil.getMenuUtil()
                    .createMenuTask(createRunnable(guild, msgChan, member),
                            menuSelectionTask, 12);

        } else {
            // Path 2: Not user, ignore command and return.
            long userId = member.getUser().getIdLong();
            long lastUserId = ytUtil.getMenuUtil().getUserId(commandName, msgChan);
            if (userId != lastUserId) {
                message.addReaction("\u274E").queue();
                return;
            }

            // Path 3: Not a valid int. Exit query and do not allow searching.
            if (!MessageUtil.checkIfInt(search)) {
                MessageUtil.sendError("Not a numerical response." +
                        " Please search for the video again.", msgChan);
                reset(msgChan);
                ytUtil.getMenuUtil().deleteMenu(msgChan, commandName);
                return;
            }

            // Path 4: Correct user and valid int. Add song.
            int index = Integer.parseInt(search);
            String url = ytUtil.getSongURL(index, msgChan, songsToChoose);
            if (url == null) {
                return;
            }
            ytUtil.addSong(guild, msgChan, commandName, member, url);
            reset(msgChan);
            ytUtil.getMenuUtil().deleteMenu(msgChan, commandName);
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
