package tree.command.music;

import net.dv8tion.jda.core.entities.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tree.command.data.MenuSelectionInfo;
import tree.command.data.MessageWrapper;
import tree.command.data.ReactionMenu;
import tree.command.util.MessageUtil;
import tree.command.util.api.YoutubeMusicUtil;
import tree.commandutil.CommandManager;
import tree.commandutil.type.MusicCommand;
import tree.db.DatabaseManager;
import tree.util.LoggerUtil;

import java.util.*;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Valued Customer on 8/12/2017.
 */
public class AddCommand implements MusicCommand {
    private DatabaseManager db = DatabaseManager.getInstance();
    private String commandName;
    private YoutubeMusicUtil ytUtil;
    private static Logger logger = LoggerFactory.getLogger(AddCommand.class);
    private Map<Guild, Map<Long, MenuSelectionInfo>> guildToUserMap;

    private Runnable createRunnable(Guild guild, MessageChannel msgChan, Member member) {
        return () -> {
                // If no choice has been selected, pick the first song to add.
                if (!guildToUserMap.containsKey(guild)) {
                    return;
                }
                Map<Long, MenuSelectionInfo> userSelectionMap =
                        guildToUserMap.get(guild);
                if (userSelectionMap.containsKey(member.getUser().getIdLong())) {
                    MenuSelectionInfo msInfo = userSelectionMap.get(member.getUser().getIdLong());
                    ytUtil.addSong(guild, msInfo.getChannel(), getCommandName(),
                            member, (String) msInfo.getListOfChoices().get(0));
                }
                deleteSelectionEntry(guild, member.getUser().getIdLong());
        };
    }

    public boolean isSelectingUser(Guild guild, long userId) {
        return guildToUserMap.get(guild).containsKey(userId);
    }

    private void addSelectionEntry(Guild guild, long userId, MenuSelectionInfo msInfo) {
        Map<Long, MenuSelectionInfo> userSelectionMap =
                guildToUserMap.get(guild);
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

    private void deleteSelectionEntry(Guild guild, long userId) {
        Map<Long, MenuSelectionInfo> userSelectionMap = guildToUserMap.get(guild);
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
        CommandManager.removeReactionMenu(guild, menuId);

        if (!task.isCancelled() || !task.isDone()) {
            task.cancel(true);
        }
    }

    public void reset(Guild guild, long userId) {
        deleteSelectionEntry(guild, userId);
    }

    public boolean hasMenu(Guild guild, long userId) {
        if (!guildToUserMap.containsKey(guild)) {
            return false;
        }
        Map<Long, MenuSelectionInfo> userInfoMap = guildToUserMap.get(guild);
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

    public boolean inSameChannel(Guild guild, long userId, MessageChannel msgChan) {
        if (!guildToUserMap.containsKey(guild)) {
            return false;
        }
        Map<Long, MenuSelectionInfo> userInfoMap = guildToUserMap.get(guild);
        if (userInfoMap.containsKey(userId)) {
            if (userInfoMap.get(userId).getChannel().equals(msgChan)) {
                return true;
            }
        }
        return false;
    }

    // Race condition. Save the userID who entered the search query first. Let them choose it for 7 seconds
    // If not, reset the search.

    public AddCommand(String commandName) {
        this.commandName = commandName;
        ytUtil = YoutubeMusicUtil.getInstance();
        guildToUserMap = new HashMap<>();
    }

    public void optionConfirmed(Guild guild, MessageChannel msgChan,
                                Member member, int option, long menuId) {

        long userId = member.getUser().getIdLong();
        Map<Long, MenuSelectionInfo> userChannelMap = guildToUserMap.get(guild);

        if (userChannelMap.containsKey(userId)) {
            MenuSelectionInfo msInfo = userChannelMap.get(userId);
            String url = ytUtil.getSongURL(option, msgChan, (List<String>) msInfo.getListOfChoices());
            ytUtil.addSong(guild, msgChan, commandName, member, url);
            reset(guild, member.getUser().getIdLong());
        }

    }


    @Override
    public void execute(Guild guild, MessageChannel msgChan, Message message, Member member, String[] args) {
        if (args.length <= 1) {
            LoggerUtil.logMessage(logger, message, "Only one argument allowed.");
            MessageUtil.sendError("Please provide a song or selection.", msgChan);
            return;
        }

        // If guild is not set in the map yet, set it.
        if (!guildToUserMap.containsKey(guild)) {
            guildToUserMap.put(guild, new HashMap<>());
        }

        if (!member.getVoiceState().inVoiceChannel()) {
            MessageUtil.sendError("You must be in a voice channel.", msgChan);
            return;
        }

        VoiceChannel voiceChan = member.getVoiceState().getChannel();
        if (!db.isAllowedVoiceChannel(guild, voiceChan)) {
            MessageUtil.sendError("I'm not allowed to join that channel!", msgChan);
            return;
        }

        String search = ytUtil.getQuery(args);
        if (!ytUtil.authorizedUser(guild, member)) {
            message.addReaction("\u274E").queue();
            return;
        }

        // Check if the user is already made a selection.
        // Continue with the load if the song is added.
        long userId = member.getUser().getIdLong();

            if (hasMenu(guild, userId)) {
                reset(guild, userId);
            }
            List<String> songsToChoose = new ArrayList<>();

            ScheduledFuture<?> newTask = ytUtil.getMenuUtil()
                    .createMenuTask(createRunnable(guild, msgChan, member),
                            null, 20);

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

            MenuSelectionInfo msInfo = new MenuSelectionInfo(msgWrapper.getMessage(),
                    msgChan, songsToChoose, newTask);
            // Create a new menu selection info class.
            addSelectionEntry(guild, member.getUser().getIdLong(),
                    msInfo);


    }

    @Override
    public String help() {
        return "``" + CommandManager.botToken + commandName +
                " [song]``: Adds the song with either a keyword search or a direct URL.";
    }

    @Override
    public String getCommandName() {
        return commandName;
    }
}
