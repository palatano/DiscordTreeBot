package tree.command.music;

import net.dv8tion.jda.core.entities.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tree.command.util.MessageUtil;
import tree.command.util.api.YoutubeMusicUtil;
import tree.commandutil.type.MusicCommand;
import tree.util.LoggerUtil;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Valued Customer on 8/15/2017.
 */
public class RequestCommand implements MusicCommand {
    private String commandName;
    private YoutubeMusicUtil ytUtil;
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
                songsToChoose = new ArrayList<>();
                // Delete the menu.
                long menuId = ytUtil.getMenuUtil().removeMenuId(msgChan.getIdLong());
                if (menuId != -1) {
                    msgChan.deleteMessageById(menuId).queue();
                }
            }
        };

        menuSelectionTask = scheduler.schedule(
                runnable, 25, TimeUnit.SECONDS);
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

    public RequestCommand(String commandName) {
        this.commandName = commandName;
        songsToChoose = new ArrayList<>();
        scheduler = Executors
                .newScheduledThreadPool(1);
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
        // Path 1: No query have been entered yet. ALLOW a non-authorized
        // user to request and bring up the selection menu. Do not allow them to
        // add though.
        if (!waitingForChoice.get()) {
            if (MessageUtil.checkIfInt(search)) {
                return;
            }
            if (menuSelectionTask != null && scheduler != null) {
                menuSelectionTask.cancel(true);
            }
            if (ytUtil.youtubeSearch(search, guild, msgChan, message,
                    member, getCommandName(), counter, songsToChoose, waitingForChoice) == -1) {
                return;
            }
            if (menuSelectionTask == null || menuSelectionTask.isCancelled() || menuSelectionTask.isDone()) {
                createScheduler(guild, msgChan, message, member);
                message.addReaction("\u2705").queue();
            }
        } else {
            // Path 2: Not user, ignore command and return.
            if (!ytUtil.authorizedUser(guild, member)) {
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
        return "Allow a non-authorized user to request a song and an authorized user to add it.";
    }

    @Override
    public String getCommandName() {
        return commandName;
    }
}
