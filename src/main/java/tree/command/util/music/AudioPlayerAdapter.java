package tree.command.util.music;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.managers.AudioManager;
import tree.command.util.MessageUtil;
import tree.util.LoggerUtil;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Valued Customer on 8/12/2017.
 */
public class AudioPlayerAdapter extends ListenerAdapter {
    private final AudioPlayerManager playerManager;
    private final Map<Long, GuildMusicManager> musicManagers;
    public static final AudioPlayerAdapter audioPlayer = new AudioPlayerAdapter();

    public static void init() {

    }

    private AudioPlayerAdapter() {
        this.musicManagers = new HashMap<>();

        this.playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);
    }

    public synchronized GuildMusicManager getGuildAudioPlayer(Guild guild) {
        long guildId = Long.parseLong(guild.getId());
        GuildMusicManager musicManager = musicManagers.get(guildId);

        if (musicManager == null) {
            musicManager = new GuildMusicManager(playerManager);
            musicManagers.put(guildId, musicManager);
        }

        guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());

        return musicManager;
//        guild.getOwner().getI
    }


    /**
     * Using the &add command, load and play a song on the channel.
     * @param channel
     * @param trackUrl
     */
    public void loadAndPlay(final TextChannel channel, final String trackUrl, Member member) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());

        playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                channel.sendMessage("Adding to queue: ``" + track.getInfo().title + "``").queue();

                play(channel.getGuild(), musicManager, track, member);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                AudioTrack firstTrack = playlist.getSelectedTrack();

                if (firstTrack == null) {
                    firstTrack = playlist.getTracks().get(0);
                }

                channel.sendMessage("Adding to queue: ``" + firstTrack.getInfo().title + "`` (first track of playlist ``" + playlist.getName() + "``)").queue();

                play(channel.getGuild(), musicManager, firstTrack, member);
            }

            @Override
            public void noMatches() {
                channel.sendMessage("Nothing found by ``" + trackUrl + "``").queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                channel.sendMessage("Could not play: " + exception.getMessage()).queue();
            }
        });
    }

    /**
     * Add the track to the queue.
     * @param guild
     * @param musicManager
     * @param track
     */
    public void play(Guild guild, GuildMusicManager musicManager, AudioTrack track, Member member) {
        connectToMusicChannel(guild.getAudioManager());
        musicManager.scheduler.queue(track, member);
    }

    /**
     * Skip the track in the queue.
     * @param channel
     */
    public void skipTrack(TextChannel channel) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
        musicManager.scheduler.nextTrack();

        channel.sendMessage("Skipped to next track.").queue();
    }

    /**
     * Connect to the voice channel specified in the audio manager.
     * @param audioManager
     */
    public static void connectToMusicChannel(AudioManager audioManager) {
        Guild guild = audioManager.getGuild();
        if (!audioManager.isConnected() && !audioManager.isAttemptingToConnect()) {
            List<VoiceChannel> voiceChannelList = guild.getVoiceChannelsByName("music", true);
            if (voiceChannelList.isEmpty()) {
                System.out.println("No #music channel found.");
                return;
            }
            // For r/trees:
            if (guild.getName().equals("/r/trees")) {
                audioManager.openAudioConnection(guild.getVoiceChannelById(346492804316397578L));
                return;
            }
            audioManager.openAudioConnection(voiceChannelList.get(0));
            return;
        }
    }

}
