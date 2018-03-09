package tree.event;

import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.guild.GenericGuildEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.core.events.message.GenericMessageEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.core.managers.AudioManager;
import tree.Config;
import tree.command.analysis.GoogleSearchCommand;
import tree.command.analysis.YoutubeCommand;
import tree.command.data.ReactionMenu;
import tree.command.music.AddCommand;
import tree.command.music.PlaylistCommand;
import tree.command.music.RequestCommand;
import tree.command.util.api.YoutubeMusicUtil;
import tree.command.util.music.AudioPlayerAdapter;
import tree.command.util.music.GuildMusicManager;
import tree.commandutil.CommandManager;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import tree.commandutil.util.CommandRegistry;
import tree.db.DatabaseManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Admin on 7/29/2017.
 */
public class TreeListener extends ListenerAdapter {
    private static final long[] TESTING_CHANNELS = {314495018079617025L, 345931676746121216L,
            337641574249005065L, 374224008922529792L};
    private DatabaseManager db = DatabaseManager.getInstance();

    private void searchOptionFromMenu(Guild guild, MessageChannel msgChan,
                                      Member member, long menuId,
                                      ReactionMenu reactionMenu, int index) {

        String commandName = reactionMenu.getCommandName();
        if (reactionMenu.isClicked()) {
            return;
        }

        if (commandName.equals("youtube")) {
            YoutubeCommand youtubeCommand = (YoutubeCommand) CommandRegistry.getCommand(commandName);

            if (youtubeCommand.isSelectingUser(guild, member)) {
                reactionMenu.clicked();
                youtubeCommand.nextOption(guild, msgChan, member, menuId);
            }
        } else if (commandName.equals("google")) {
            GoogleSearchCommand googleSearchCommand = (GoogleSearchCommand) CommandRegistry.getCommand(commandName);

            if (googleSearchCommand.isSelectingUser(guild, member)) {
                reactionMenu.clicked();
                googleSearchCommand.nextOptionSelected(guild, msgChan, member);
            }
        } else if (commandName.equals("list")) {
            PlaylistCommand playlistCommand =
                    (PlaylistCommand) CommandRegistry.getCommand(commandName);
            if (playlistCommand.isAllowedUser(guild, member)) {
                reactionMenu.clicked();
                playlistCommand.next(guild, msgChan, member);
            }

        }
    }

    private void musicOptionFromMenu(Guild guild, MessageChannel msgChan,
                                       Member member, long menuId,
                                       ReactionMenu reactionMenu, int index) {
        String commandName = reactionMenu.getCommandName();
        if (reactionMenu.isClicked()) {
            return;
        }

        // Figure out where to forward the command with the option given. The return value can be an int.
        if (commandName.equals("add")) {
            AddCommand addCommand = (AddCommand) CommandRegistry.getCommand(commandName);
            if (addCommand.isSelectingUser(guild, member.getUser().getIdLong())) {
                addCommand.optionConfirmed(guild, msgChan, member, index, menuId);
            }
        } else if (commandName.equals("req")) {
            RequestCommand requestCommand = (RequestCommand) CommandRegistry.getCommand(commandName);
            YoutubeMusicUtil ytUtil = YoutubeMusicUtil.getInstance();

            if (!ytUtil.authorizedUser(guild, member)) {
                return;
            }

            long userId = reactionMenu.getUserId();
            Member memb = guild.getMemberById(userId);
            if (index == -2) {
                reactionMenu.clicked();
                requestCommand.onConfirmation(guild, msgChan, member);
            } else {
                reactionMenu.clicked();
                requestCommand.optionConfirmed(guild, msgChan, memb, index, menuId);
            }
        }

    }

    public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
        // Get the event.
        if (event == null || event.getChannel() == null) {
            return;
        }

        // If this is the bot, ignore.
        if (event.getUser().isBot()) {
            return;
        }

        long menuId = event.getMessageIdLong();
        Member member = event.getMember();
        long userId = event.getUser().getIdLong();
        Guild guild = event.getGuild();
        MessageChannel msgChan = event.getChannel();

        if (!CommandManager.reactionMenuMap.containsKey(guild)) {
            CommandManager.reactionMenuMap.put(guild, new HashMap<>());
        }
        Map<Long, ReactionMenu> reactionMenuMap = CommandManager.reactionMenuMap.get(guild);

        // Get the reaction, it should be a one, two, or three that we care about.
        MessageReaction.ReactionEmote reaction = event.getReactionEmote();
        if (reaction.getName().equals("\u0031\u20E3")) {
            // If the user is not in the map, return.
            if (!reactionMenuMap.containsKey(menuId)) {
                return;
            }
            ReactionMenu reactionMenu = reactionMenuMap.get(menuId);
            musicOptionFromMenu(guild, msgChan, member,
                    menuId, reactionMenu, 1);

        } else if (reaction.getName().equals("\u0032\u20E3")) {
            // If the user is not in the map, return.
            if (!reactionMenuMap.containsKey(menuId)) {
                return;
            }
            ReactionMenu reactionMenu = reactionMenuMap.get(menuId);
            musicOptionFromMenu(guild, msgChan, member,
                    menuId, reactionMenu, 2);

        } else if (reaction.getName().equals("\u0033\u20E3")) {
            // If the user is not in the map, return.
            if (!reactionMenuMap.containsKey(menuId)) {
                return;
            }
            ReactionMenu reactionMenu = reactionMenuMap.get(menuId);
            musicOptionFromMenu(guild, msgChan, member,
                    menuId, reactionMenu, 3);

        } else if (reaction.getName().equals("\u23F9")) {
            // If the user is not in the map, return.
//            if (!reactionMenuMap.containsKey(menuId)) {
//                return;
//            }
//            ReactionMenu reactionMenu = reactionMenuMap.get(menuId);
//            searchOptionFromMenu(guild, msgChan, member,
//                    menuId, reactionMenu, -1);

        } else if (reaction.getName().equals("⏭")) {
            // If the user is not in the map, return.
            if (!reactionMenuMap.containsKey(menuId)) {
                return;
            }
            ReactionMenu reactionMenu = reactionMenuMap.get(menuId);
            searchOptionFromMenu(guild, msgChan, member,
                    menuId, reactionMenu, -1);
        }

        else if (reaction.getName().equals("\u2611")) {
            if (!reactionMenuMap.containsKey(menuId)) {
                return;
            }
            ReactionMenu reactionMenu = reactionMenuMap.get(menuId);
            musicOptionFromMenu(guild, msgChan, member, menuId, reactionMenu, -2);
        }

    }

    public void onMessageReceived(MessageReceivedEvent event) {
        if (event == null || event.getChannel() == null) {
            return;
        }

        Message msg = event.getMessage();
        if (!msg.getContentRaw().startsWith(";")) {
            return;
        }

        // If testing only, only allow commands in testing server.
        if (Config.CONFIG.isTesting()) {
            if (!event.getChannelType().isGuild() || !testingOnly(event.getTextChannel())) {
                return;
            }
        } else {
            if (!event.getChannelType().isGuild() ||
                    !db.isAllowedTextChannel(event.getGuild(), event.getTextChannel()) ||
                    testingOnly(event.getTextChannel())) {
                return;
            }
        }

        CommandManager.messageCommand(msg);
    }

    private boolean testingOnly(MessageChannel msgChan) {
        for (long id : TESTING_CHANNELS) {
            if (msgChan.getIdLong() == id) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
        Guild guild = event.getGuild();
        VoiceChannel channel = event.getChannelLeft();
        Member member = guild.getSelfMember();
        List<Member> memberList = channel.getMembers();
        // Pause the player automatically.
        AudioPlayerAdapter playerAdapter = AudioPlayerAdapter.audioPlayerAdapter;
        GuildMusicManager musicManager = playerAdapter.getGuildAudioPlayer(guild);
        boolean isPaused = musicManager.player.isPaused();
        if (memberList.size() == 1 && memberList.get(0).equals(member) && !isPaused) {
            musicManager.player.setPaused(true);
            String notification = "All users have left **" + channel.getName() +
                    "**, player is now paused. Type ``;unpause`` to unpause.";
            MessageChannel msgChan = musicManager.scheduler.getLastTextChannel();
            msgChan.sendMessage(notification).queue();
        }
    }
}
