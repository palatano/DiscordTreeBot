package tree.command.music;

import net.dv8tion.jda.core.entities.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tree.Config;
import tree.command.data.ReactionMenu;
import tree.command.util.music.AudioPlayerAdapter;
import tree.command.util.music.GuildMusicManager;
import tree.command.util.music.TrackScheduler;
import tree.commandutil.CommandManager;
import tree.commandutil.type.MusicCommand;
import tree.db.DatabaseManager;
import tree.util.LoggerUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by Valued Customer on 8/12/2017.
 */
public class PlaylistCommand implements MusicCommand {
    private String commandName;
    private static Logger logger = LoggerFactory.getLogger(AddCommand.class);
    private AudioPlayerAdapter playerAdapter;
    private ScheduledExecutorService scheduler;
    private Map<Guild, GuildPlaylistInfo> guildInfoMap;
    private static int numberPosts;
    private static final int MAX_PLAYLIST_POSTS = 8;

    private class GuildPlaylistInfo {
        private MessageChannel msgChan;
        private boolean playlistTaskStarted;
        private boolean automaticPosting;
        private ScheduledFuture<?> task;
        private Message menu;
        private int page = 1;

        public int getPage() {
            return page;
        }

        public void setPage(int page) {
            this.page = page;
        }

        public Message getMenu() {
            return menu;
        }

        public void setMsg(Message menu) {
            this.menu = menu;
        }

        GuildPlaylistInfo(MessageChannel msgChan, boolean playlistTaskStarted, boolean automaticPosting, ScheduledFuture<?> task) {
            this.msgChan = msgChan;
            this.playlistTaskStarted = playlistTaskStarted;
            this.automaticPosting = automaticPosting;
            this.task = task;
        }

        public MessageChannel getMsgChan() {
            return msgChan;
        }

        public boolean isPlaylistTaskStarted() {
            return playlistTaskStarted;
        }

        public void setAutomaticPosting(boolean val) {
            automaticPosting = val;
        }

        public void setMsgChan(MessageChannel msgChan) {
            this.msgChan = msgChan;
        }

        public boolean isAutomaticPosting() {
            return automaticPosting;
        }

        public ScheduledFuture<?> getTask() {
            return task;
        }
    }

    public PlaylistCommand(String commandName) {
        this.commandName = commandName;
        playerAdapter = AudioPlayerAdapter.audioPlayerAdapter;
        scheduler = Executors.newScheduledThreadPool(1);
        guildInfoMap = new HashMap<>();
    }

//    private class

    private class PlaylistRunnable implements Runnable {
        private Guild guild;
        private MessageChannel msgChan;
        private Message message;
        private Member member;

        private PlaylistRunnable(Guild guild, MessageChannel msgChan, Message message, Member member) {
            this.guild = guild;
            this.msgChan = msgChan;
            this.message = message;
            this.member = member;
        }

        @Override
        public void run() {
            // If the queue is empty, don't post the songlist.
            GuildMusicManager musicManager = AudioPlayerAdapter.audioPlayerAdapter
                    .getGuildAudioPlayer(guild);
            GuildPlaylistInfo info = guildInfoMap.get(guild);

            // If queue is empty OR someone wanted to disable the automatic playlist,
            // have a boolean preventing it from posting.
            if (musicManager.player.isPaused() ||
                    musicManager.scheduler.isEmpty() ||
                    !info.isAutomaticPosting()) {
                return;
            }

            listSongs(guild, info.getMsgChan(), message, member);
        }
    }

    public boolean isAllowedUser(Guild guild, Member member) {
        return DatabaseManager.getInstance().hasMusicRole(guild, member);
    }

    public void next(Guild guild, MessageChannel msgChan, Member member) {
        GuildPlaylistInfo info = guildInfoMap.get(guild);
        GuildMusicManager manager = playerAdapter.getGuildAudioPlayer(guild);
        if (info == null) {
            return;
        }

        // Pages have have ten results on them. The first page should have
        // the currently playing song.
        int page = info.getPage();

        TrackScheduler scheduler = manager.scheduler;


    }

    private ScheduledFuture<?> schedulePlaylist(Guild guild, MessageChannel msgChan, Message message, Member member) {
        // Create a new task, but make sure the guild is updated.
        int time = 20;
        ScheduledFuture<?> task = scheduler.scheduleWithFixedDelay(new PlaylistRunnable(guild, msgChan, message, member),
                time, time, TimeUnit.MINUTES);
        return task;
    }

    private void listSongs(Guild guild, MessageChannel msgChan,
                           Message message, Member member) {
        GuildMusicManager musicManager = AudioPlayerAdapter.audioPlayerAdapter
                .getGuildAudioPlayer(guild);
        GuildPlaylistInfo info = guildInfoMap.get(guild);

        String songList = musicManager.scheduler.printSongList();
        Message msg = msgChan.sendMessage(songList).complete();
        info.setMsg(msg);

        // After doing that, add the reaction and create the reaction menu.
        ReactionMenu menu = new ReactionMenu(commandName, member.getUser().getIdLong(), msgChan);
        CommandManager.addReactionMenu(guild, msg.getIdLong(), menu);
        msg.addReaction("⏭");

    }

    @Override
    public void execute(Guild guild, MessageChannel msgChan, Message message, Member member, String[] args) {
        String commandWithToken = CommandManager.botToken + getCommandName();
        if (args.length < 1 || args.length > 2) {
            LoggerUtil.logMessage(logger, message, commandWithToken +
                    ", " + commandWithToken + " off, or " + commandWithToken + " on is only allowed.");
            message.addReaction("\u274E").queue();
            return;
        }
        if (args.length == 2 && args[1] != null) {
            GuildPlaylistInfo info = guildInfoMap.get(guild);
            if (info == null) {
                message.addReaction("\u274E").queue();
                return;
            }

            if (args[1].equals("off")) {
                info.setAutomaticPosting(false);
                message.addReaction("\u2705").queue();
            } else if (args[1].equals("on")) {
                info.setAutomaticPosting(true);
                message.addReaction("\u2705").queue();
            } else {
                LoggerUtil.logMessage(logger, message, "Not a valid response. " + commandWithToken + " on" +
                        " OR " + commandWithToken + " off is allowed.");
                message.addReaction("\u274E").queue();
            }
        } else {
            // First, we need to check if the music player is paused or not
            // playing music.
            GuildMusicManager musicManager =
                    AudioPlayerAdapter.audioPlayerAdapter
                            .getGuildAudioPlayer(guild);

            // Start scheduler.
            GuildPlaylistInfo info = guildInfoMap.get(guild);

//            guildInfoMap.put(guild, msgChan);
            if (info == null) {
              ScheduledFuture<?> task =
                        schedulePlaylist(guild, msgChan, message, member);
                info = new GuildPlaylistInfo(msgChan, true, true, task);
                guildInfoMap.put(guild, info);
                // If the playlist is empty, make sure its stated once.
                if (!musicManager.scheduler.isEmpty()) {
                    listSongs(guild, msgChan, message, member);
                }
            } else {
                info.setMsgChan(msgChan);
                listSongs(guild, msgChan, message, member);
            }
        }
    }

    @Override
    public String help() {
        return "``" + CommandManager.botToken + commandName + "``: Returns the current song list." +
                " Turn on/off automatic playlist posting with ;list [on|off]";
    }

    @Override
    public String getCommandName() {
        return commandName;
    }

}
