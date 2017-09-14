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
    private Map<Guild, Map<Long, MenuSelectionInfo>> guildToMenuMap;
    private Map<Guild, Map<Long, String>> guildSongConfirmationMap;

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
        if (!guildToMenuMap.containsKey(guild) || !guildSongConfirmationMap.containsKey(guild)) {
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

    public void onConfirmation(Guild guild, MessageChannel msgChan, Member member) {
        long userId = member.getUser().getIdLong();
        String url = guildSongConfirmationMap.get(guild).get(userId);
        ytUtil.addSong(guild, msgChan, commandName, member, url);
        reset(guild, userId);
        guildSongConfirmationMap.get(guild).remove(userId);
    }

    private void addSelectionEntry(Guild guild, long userId, MenuSelectionInfo msInfo) {
        Map<Long, MenuSelectionInfo> userSelectionMap =
                guildToMenuMap.get(guild);
        userSelectionMap.put(userId, msInfo);
    }

    public void optionConfirmed(Guild guild, MessageChannel msgChan,
                                Member member, int option, long menuId) {

        long userId = member.getUser().getIdLong();
        Map<Long, MenuSelectionInfo> userChannelMap = guildToMenuMap.get(guild);

        MenuSelectionInfo msInfo = userChannelMap.get(userId);
        String url = ytUtil.getSongURL(option, msgChan,
                (List<String>) msInfo.getSongsToChoose());

        guildSongConfirmationMap.get(guild).put(userId, url);
        reset(guild, userId);

        // Create the menu for confirmation.
        Iterator<Long> iter = Config.getGuildAdmins().get(guild.getIdLong()).iterator();
        String confirmationString = "";
        if (iter.hasNext()) {
            confirmationString += "**" + guild.getRoleById(iter.next()).getName() + ":** ";
        } else {
            confirmationString += "**Authorized Users:** ";
        }
        confirmationString += "Please confirm the song, requested by ``" + member.getEffectiveName() + "``";

        Message menu = msgChan.sendMessage(confirmationString).complete();
        menu.addReaction("\u2611").queue();
        MenuSelectionInfo newMsInfo =
                new MenuSelectionInfo(menu, msgChan, msInfo.getSongsToChoose(), msInfo.getTask());
        addSelectionEntry(guild, userId, newMsInfo);

        ReactionMenu reactionMenu = new ReactionMenu(commandName, userId, msgChan);
        CommandManager.addReactionMenu(guild, menu.getIdLong(), reactionMenu);
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
        CommandManager.removeReactionMenu(guild, menuId);

        if (!task.isCancelled() || !task.isDone()) {
            task.cancel(true);
        }
    }

    public RequestCommand(String commandName) {
        this.commandName = commandName;
        guildToMenuMap = new HashMap<>();
        ytUtil = YoutubeMusicUtil.getInstance();
        guildSongConfirmationMap = new HashMap<>();
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


    private void checkIfGuildExists(Guild guild) {
        if (!guildToMenuMap.containsKey(guild)) {
            guildToMenuMap.put(guild, new HashMap<>());
        }
        if (!guildSongConfirmationMap.containsKey(guild)) {
            guildSongConfirmationMap.put(guild, new HashMap<>());
        }
    }

    @Override
    public void execute(Guild guild, MessageChannel msgChan, Message message, Member member, String[] args) {
        if (args.length <= 1) {
            LoggerUtil.logMessage(logger, message, "Only one argument allowed.");
            MessageUtil.sendError("Please provide a song to add.", msgChan);
            return;
        }

        checkIfGuildExists(guild);

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

            Message menu = msInfo.getMenu();
            ReactionMenu reactionMenu = new ReactionMenu(commandName, userId,
                msInfo.getChannel());

            CommandManager.addReactionMenu(guild, msInfo.getMenu().getIdLong(),
                    reactionMenu);
            menu.addReaction("\u0031\u20E3").queue();
            menu.addReaction("\u0032\u20E3").queue();
            menu.addReaction("\u0033\u20E3").queue();

    }

    @Override
    public String help() {
        return "``" + CommandManager.botToken + commandName +
                " [song]``: Allow a non-authorized user to request a song and an authorized user to add it.";
    }

    @Override
    public String getCommandName() {
        return commandName;
    }
}
