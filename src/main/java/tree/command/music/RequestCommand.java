package tree.command.music;

import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.entities.impl.MessageImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tree.Config;
import tree.command.data.MenuSelectionInfo;
import tree.command.data.MessageWrapper;
import tree.command.data.ReactionMenu;
import tree.command.util.MessageUtil;
import tree.command.util.api.YoutubeMusicUtil;
import tree.commandutil.CommandManager;
import tree.commandutil.type.MusicCommand;
import tree.util.LoggerUtil;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Valued Customer on 8/15/2017.
 */
public class RequestCommand implements MusicCommand {
    private String commandName;
    private YoutubeMusicUtil ytUtil;
    private static Logger logger = LoggerFactory.getLogger(AddCommand.class);
    private AtomicBoolean waitingForChoice;
    private Map<Guild, Map<Long, MenuSelectionInfo>> guildToMenuMap;

    private Runnable createRunnable(Guild guild, MessageChannel msgChan, Member member) {
        return () -> {
            reset(guild, member.getUser().getIdLong());
            MessageUtil.sendError("Menu timed out.", msgChan);
        };
    }

    public void reset(Guild guild, long userId) {
        deleteSelectionEntry(guild, userId);
    }

    public boolean hasMenu(Guild guild, long userId) {
        if (!guildToMenuMap.containsKey(guild)) {
            return false;
        }
        Map<Long, MenuSelectionInfo> userInfoMap = guildToMenuMap.get(guild);
        if (userInfoMap.containsKey(userId)) {
            if (userInfoMap.get(userId).getMenu().getIdLong() == 0) {
                userInfoMap.remove(userId);
                System.out.println("Error, this should not happen.");
                return false;
            }
            return true;
        }
        return false;
    }

    private void addSelectionEntry(Guild guild, long userId, MenuSelectionInfo msInfo) {
        Map<Long, MenuSelectionInfo> userSelectionMap =
                guildToMenuMap.get(guild);
        userSelectionMap.put(userId, msInfo);

        Message message = msInfo.getMenu();
        ReactionMenu reactionMenu = new ReactionMenu(commandName, userId,
                msInfo.getChannel());

        CommandManager.addReactionMenu(guild, msInfo.getMenu().getIdLong(),
                reactionMenu);
        message.addReaction("\u0031\u20E3").queue();
        message.addReaction("\u0032\u20E3").queue();
        message.addReaction("\u0033\u20E3").queue();
    }

    public void optionConfirmed(Guild guild, MessageChannel msgChan,
                                Member member, int option, long menuId) {

        long userId = member.getUser().getIdLong();
        Map<Long, MenuSelectionInfo> userChannelMap = guildToMenuMap.get(guild);

        MenuSelectionInfo msInfo = userChannelMap.get(userId);
        String url = ytUtil.getSongURL(option, msgChan,
                (List<String>) msInfo.getSongsToChoose());
        ytUtil.addSong(guild, msgChan, commandName, member, url);
        reset(guild, member.getUser().getIdLong());
    }

    private void deleteSelectionEntry(Guild guild, long userId) {
        Map<Long, MenuSelectionInfo> userSelectionMap = guildToMenuMap.get(guild);
        if (!userSelectionMap.containsKey(userId)) {
            System.out.println("Bot is attempting to remove a non-existent user.");
            return;
        }
        MenuSelectionInfo msInfo = userSelectionMap.get(userId);
        ScheduledFuture<?> task = msInfo.getTask();
        long menuId = msInfo.getMenu().getIdLong();
        MessageChannel msgChan = msInfo.getChannel();
        if (menuId != 0) {
            msgChan.deleteMessageById(menuId).queue();
        }
        userSelectionMap.remove(userId);
        if (!task.isCancelled() || !task.isDone()) {
            task.cancel(true);
        }
    }

    public RequestCommand(String commandName) {
        this.commandName = commandName;
        guildToMenuMap = new HashMap<>();
        ytUtil = YoutubeMusicUtil.getInstance();
        waitingForChoice = new AtomicBoolean(false);
    }

    public boolean inSameChannel(Guild guild, long userId, MessageChannel msgChan) {
        if (!guildToMenuMap.containsKey(guild)) {
            return false;
        }
        Map<Long, MenuSelectionInfo> userInfoMap = guildToMenuMap.get(guild);
        if (userInfoMap.containsKey(userId)) {
            if (userInfoMap.get(userId).getChannel().equals(msgChan)) {
                return true;
            }
        }
        return false;
    }

    private boolean menusInChannel(Guild guild, MessageChannel msgChan) {
        if (!guildToMenuMap.containsKey(guild)) {
            return false;
        }
        Map<Long, MenuSelectionInfo> menuInfoMap = guildToMenuMap.get(guild);
        for (MenuSelectionInfo msInfo : menuInfoMap.values()) {
            if (msInfo.getChannel().equals(msgChan)) {
                return true;
            }
        }
        return false;
    }
//
//    private long retrieveNextSelection(Guild guild, MessageChannel msgChan) {
//        // Get the selection that is in the right channel.
//        ConcurrentLinkedQueue<Long> queue = guildQueueMap.get(guild);
//        Map<Long, MenuSelectionInfo> userSelectionMap = guildToMenuMap.get(guild);
//        for (long userId : queue) {
//            MenuSelectionInfo msInfo = userSelectionMap.get(userId);
//            if (msInfo.getChannel().equals(msgChan)) {
//                return userId;
//            }
//        }
//        return -1;
//    }

    @Override
    public void execute(Guild guild, MessageChannel msgChan, Message message, Member member, String[] args) {
        if (args.length <= 1) {
            LoggerUtil.logMessage(logger, message, "Only one argument allowed.");
            MessageUtil.sendError("Please provide a song to add.", msgChan);
            return;
        }

        if (!guildToMenuMap.containsKey(guild)) {
            guildToMenuMap.put(guild, new HashMap<>());
        }

        if (!member.getVoiceState().inVoiceChannel()) {
            MessageUtil.sendError("You must be in a voice channel.", msgChan);
            return;
        }

        VoiceChannel voiceChan = member.getVoiceState().getChannel();
        if (!Config.isAllowedVoiceChannel(guild, voiceChan.getIdLong())) {
            MessageUtil.sendError("I'm not allowed to join that channel!", msgChan);
            return;
        }

        // When a user is entering a number selection, make sure that a menu exists in the first place.

        String search = ytUtil.getQuery(args);
        long userId = member.getUser().getIdLong();
        // Path 1: No query have been entered yet. ALLOW a non-authorized
        // user to request and bring up the selection menu. Do not allow them to
        // add though.

//        if (MessageUtil.checkIfInt(search)) {
//            // If not authorized OR there is no selection made yet.
//            if (!ytUtil.authorizedUser(guild, member)) {
//                message.addReaction("\u274E").queue();
//                return;
//            }
//
//            if (!menusInChannel(guild, msgChan)) {
//                MessageUtil.sendError("There are no requests in this channel.", msgChan);
//                return;
//            }
//
//            long nextId = retrieveNextSelection(guild, msgChan);
//            if (nextId == -1) {
//                MessageUtil.sendError("There are no menus in this channel to choose from.", msgChan);
//                return;
//            }
//            MenuSelectionInfo msInfo = guildToMenuMap.get(guild).get(nextId);
//            int index = Integer.parseInt(search);
//            String url = ytUtil.getSongURL(index, msgChan, (List<String>) msInfo.getSongsToChoose());
//            if (url == null) {
//                return;
//            }
//            ytUtil.addSong(guild, msgChan, commandName, member, url);
//            reset(guild, nextId);
//        } else {

            // Check if a previous menu exists.
            // If person has a menu but wants to re-enter it again, allow the if condition to fail.
            if (hasMenu(guild, userId)) {
                reset(guild, userId);
            }

            ScheduledFuture<?> newTask = ytUtil.getMenuUtil()
                    .createMenuTask(createRunnable(guild, msgChan, member),
                            null, 60);
            List<String> songsToChoose = new ArrayList<>();

            // Allow the user to keep searching for a song
            // until a number command is made.
            // Return if direct URL search or error has occurred.
            MessageWrapper msgWrapper = new MessageWrapper();
            if (ytUtil.youtubeSearch(search, guild, msgChan, msgWrapper,
                    member, getCommandName(), new AtomicInteger(), songsToChoose) == -1) {
                if (!newTask.isCancelled() || !newTask.isDone()) {
                    newTask.cancel(true);
                }
                return;
            }

            MenuSelectionInfo msInfo = new MenuSelectionInfo(msgWrapper.getMessage(), msgChan,
                    songsToChoose, newTask);
            // Create a new menu selection info class.
            addSelectionEntry(guild, member.getUser().getIdLong(),
                    msInfo);


    }

    @Override
    public String help() {
        return "``" + CommandManager.botToken + commandName + " [song]``: Allow a non-authorized user to request a song and an authorized user to add it.";
    }

    @Override
    public String getCommandName() {
        return commandName;
    }
}
