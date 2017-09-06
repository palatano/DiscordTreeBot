package tree.command.music;

import net.dv8tion.jda.core.entities.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tree.Config;
import tree.command.data.MenuSelectionInfo;
import tree.command.util.MessageUtil;
import tree.command.util.api.YoutubeMusicUtil;
import tree.commandutil.type.MusicCommand;
import tree.util.LoggerUtil;

import java.util.*;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Valued Customer on 8/12/2017.
 */
public class AddCommand implements MusicCommand {
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
                            member, (String) msInfo.getSongsToChoose().get(0));
                }
                deleteSelectionEntry(guild, member.getUser().getIdLong());
        };
    }

    private void addSelectionEntry(Guild guild, long userId, MenuSelectionInfo msInfo) {
        if (!guildToUserMap.containsKey(guild)) {
            guildToUserMap.put(guild, new HashMap<>());
        }
        Map<Long, MenuSelectionInfo> userSelectionMap =
                guildToUserMap.get(guild);
        userSelectionMap.put(userId, msInfo);
    }

    private void deleteSelectionEntry(Guild guild, long userId) {
        if (!guildToUserMap.containsKey(guild)) {
            System.out.println("Bot is attempting to remove a user and its selection from" +
                    " a non-existent guild.");
            return;
        }
        Map<Long, MenuSelectionInfo> userSelectionMap = guildToUserMap.get(guild);
        if (!userSelectionMap.containsKey(userId)) {
            System.out.println("Bot is attempting to remove a non-existent user.");
            return;
        }
        MenuSelectionInfo msInfo = userSelectionMap.get(userId);
        ScheduledFuture<?> task = msInfo.getTask();
        long menuId = msInfo.getMenuId();
        MessageChannel msgChan = msInfo.getChannel();
        if (menuId != 0) {
            msgChan.deleteMessageById(menuId).queue();
        }
        userSelectionMap.remove(userId);
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
            if (userInfoMap.get(userId).getMenuId() == 0) {
                userInfoMap.remove(userId);
                System.out.println("Error, this should not happen.");
                return false;
//                throw new IllegalStateException("Error, this should not happen.");
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


    @Override
    public void execute(Guild guild, MessageChannel msgChan, Message message, Member member, String[] args) {
        if (args.length <= 1) {
            LoggerUtil.logMessage(logger, message, "Only one argument allowed.");
            MessageUtil.sendError("Please provide a song or selection.", msgChan);
            return;
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

        String search = ytUtil.getQuery(args);
        if (!ytUtil.authorizedUser(guild, member)) { // || ytUtil.menuIsOpen(this, msgChan)
            message.addReaction("\u274E").queue();
            return;
        }

        // Check if the user is already made a selection.
        // Continue with the load if the song is added.
        long userId = member.getUser().getIdLong();
        if (MessageUtil.checkIfInt(search)) {
            if (!hasMenu(guild, userId)) {
                MessageUtil.sendError(
                        "You need to search for the song first.",
                        msgChan);
                return;
            }
            // Check if the user has a selection menu running.
            Map<Long, MenuSelectionInfo> userChannelMap = guildToUserMap.get(guild);
            long channelId = msgChan.getIdLong();
            if (userChannelMap.containsKey(userId)) {
                MenuSelectionInfo msInfo = userChannelMap.get(userId);
                if (msInfo.getChannel().getIdLong() != channelId) {
                    MessageUtil.sendError("You must select in the same channel" +
                            " where the menu is posted.", msgChan);
                    return;
                }
                int index = Integer.parseInt(search);
                String url = ytUtil.getSongURL(index, msgChan, (List<String>) msInfo.getSongsToChoose());
                if (url == null) {
                    return;
                }
                ytUtil.addSong(guild, msgChan, commandName, member, url);
                reset(guild, member.getUser().getIdLong());
            } else {
                MessageUtil.sendError(
                        "You need to search for the song first.",
                        msgChan);
                return;
            }
        } else {

            if (hasMenu(guild, userId)) {
                reset(guild, userId);
            }
            AtomicLong menuId = new AtomicLong(0);

            ScheduledFuture<?> newTask = ytUtil.getMenuUtil()
                    .createMenuTask(createRunnable(guild, msgChan, member),
                            null, 20);
            List<String> songsToChoose = new ArrayList<>();

            // Allow the user to keep searching for a song
            // until a number command is made.
            // Return if direct URL search or error has occurred.
            if (ytUtil.youtubeSearch(search, guild, msgChan, message,
                    member, getCommandName(), new AtomicInteger(), songsToChoose, menuId) == -1) {
                if (!newTask.isCancelled() || !newTask.isDone()) {
                    newTask.cancel(true);
                }
                return;
            }

            MenuSelectionInfo msInfo = new MenuSelectionInfo(menuId.get(), msgChan,
                    songsToChoose, newTask);
            // Create a new menu selection info class.
            addSelectionEntry(guild, member.getUser().getIdLong(),
                    msInfo);

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
