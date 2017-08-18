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
    private ScheduledFuture<?> menuSelectionTask;

    private Runnable createRunnable(Guild guild, MessageChannel msgChan, Member member) {
        return () -> {
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

//    public ScheduledFuture<?> getMenuSelectionTask() {
//        return menuSelectionTask;
//    }
//
//    public AtomicBoolean isWaitingForChoice() {
//        return waitingForChoice;
//    }

    // Race condition. Save the userID who entered the search query first. Let them choose it for 7 seconds
    // If not, reset the search.

    public RequestCommand(String commandName) {
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

//        if (ytUtil.menuIsOpen(this, msgChan)) {
//            message.addReaction("\u274E").queue();
//            return;
//        }


        String search = ytUtil.getQuery(args);
        // Path 1: No query have been entered yet. ALLOW a non-authorized
        // user to request and bring up the selection menu. Do not allow them to
        // add though.
        if (!waitingForChoice.get()) {
            if (MessageUtil.checkIfInt(search)) {
                message.addReaction("\u274E").queue();
                return;
            }

            if (ytUtil.youtubeSearch(search, guild, msgChan, message,
                    member, getCommandName(), counter, songsToChoose, waitingForChoice) == -1) {
                return;
            }

            menuSelectionTask = ytUtil.getMenuUtil().createMenuTask(createRunnable(guild, msgChan, member),
                    menuSelectionTask, 15);
        } else {
            // Path 2: Not user, ignore command and return.
            long lastUserId = ytUtil.getMenuUtil().getUserId(commandName, msgChan);
            if (!ytUtil.authorizedUser(guild, member) || lastUserId == -1) {
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
        return "Allow a non-authorized user to request a song and an authorized user to add it.";
    }

    @Override
    public String getCommandName() {
        return commandName;
    }
}
