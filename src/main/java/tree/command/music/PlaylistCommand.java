package tree.command.music;

import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.managers.AudioManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tree.command.util.MessageUtil;
import tree.command.util.music.AudioPlayerAdapter;
import tree.command.util.music.GuildMusicManager;
import tree.commandutil.CommandManager;
import tree.commandutil.type.MusicCommand;
import tree.util.LoggerUtil;

import java.sql.Time;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private AudioPlayerAdapter audioPlayer;
    private static Logger logger = LoggerFactory.getLogger(AddCommand.class);
    private ScheduledExecutorService scheduler;
    private boolean playlistTaskStarted;
    private Map<Guild, MessageChannel> guildChannelMap;
    private boolean automaticPosting = true;
    private ScheduledFuture<?> task;
    private static int numberPosts;
    private static final int MAX_PLAYLIST_POSTS = 8;

    public PlaylistCommand(String commandName) {
        this.commandName = commandName;
        audioPlayer = AudioPlayerAdapter.audioPlayer;
        scheduler = Executors.newScheduledThreadPool(1);
        guildChannelMap = new HashMap<>();
    }

    private class PlaylistRunnable implements Runnable {
        private Guild guild;
        private MessageChannel msgChan;
        private Message message;
        private Member member;

        public PlaylistRunnable(Guild guild, MessageChannel msgChan, Message message, Member member) {
            this.guild = guild;
            this.msgChan = msgChan;
            this.message = message;
            this.member = member;
        }

        @Override
        public void run() {
            // If the queue is empty, don't post the songlist.
            GuildMusicManager musicManager = AudioPlayerAdapter.audioPlayer.getGuildAudioPlayer(guild);
            // If queue is empty OR someone wanted to disable the automatic playlist,
            // have a boolean preventing it from posting.
            if (musicManager.scheduler.isEmpty() || !automaticPosting) {
                return;
            }

            listSongs(guild, guildChannelMap.get(guild), message, member);
            if (++numberPosts >= MAX_PLAYLIST_POSTS) {
                numberPosts = 0;
                playlistTaskStarted = false;
            }
        }
    }

    private void schedulePlaylist(Guild guild, MessageChannel msgChan, Message message, Member member) {
        // Create a new task, but make sure the guild is updated.
        task = scheduler.scheduleWithFixedDelay(new PlaylistRunnable(guild, msgChan, message, member),
                0, 15, TimeUnit.MINUTES);
    }

    private void listSongs(Guild guild, MessageChannel msgChan, Message message, Member member) {
        GuildMusicManager musicManager = AudioPlayerAdapter.audioPlayer.getGuildAudioPlayer(guild);
        String songList = musicManager.scheduler.printSongList();
        msgChan.sendMessage(songList).queue();
    }

    @Override
    public void execute(Guild guild, MessageChannel msgChan, Message message, Member member, String[] args) {
        String commandWithToken = CommandManager.botToken + getCommandName();
        if (args.length < 1 || args.length > 2) {
            LoggerUtil.logMessage(logger, message, commandWithToken +
                    ", " + commandWithToken + " off, or " + commandWithToken + " on is only allowed.");
            //MessageUtil.sendError("Not a valid response. ``&list on`` OR ``&list off`` is allowed.", msgChan);
            message.addReaction("\u274E").queue();
            return;
        }
        if (args.length == 2 && args[1] != null) {
            if (args[1].equals("off")) {
                automaticPosting = false;
                message.addReaction("\u2705").queue();
            } else if (args[1].equals("on")) {
                automaticPosting = true;
                message.addReaction("\u2705").queue();
            } else {
                LoggerUtil.logMessage(logger, message, "Not a valid response. " + commandWithToken + " on" +
                        " OR " + commandWithToken + " off is allowed.");
                //MessageUtil.sendError("Not a valid response. ``&list on`` OR ``&list off`` is allowed.", msgChan);
                message.addReaction("\u274E").queue();
            }
        } else {
            // Start scheduler.
            guildChannelMap.put(guild, msgChan);
            if (!playlistTaskStarted) {
                if (task != null &&
                        (!task.isCancelled() || !task.isDone())) {
                    task.cancel(true);
                }
                schedulePlaylist(guild, msgChan, message, member);
                playlistTaskStarted = true;
                // If the playlist is empty, make sure its stated once.
                GuildMusicManager musicManager = AudioPlayerAdapter.audioPlayer.getGuildAudioPlayer(guild);
                if (musicManager.scheduler.isEmpty()) {
                    listSongs(guild, msgChan, message, member);
                }
            } else {
                listSongs(guild, msgChan, message, member);
            }
        }
    }

    @Override
    public String help() {
        return "Returns the current song list.";
    }

    @Override
    public String getCommandName() {
        return commandName;
    }

}
