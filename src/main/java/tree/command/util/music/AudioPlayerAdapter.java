package tree.command.util.music;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import javafx.util.Pair;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.managers.AudioManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tree.TreeMain;
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
    public static final AudioPlayerAdapter audioPlayerAdapter = new AudioPlayerAdapter();
    private static Logger log = LoggerFactory.getLogger(AudioPlayerAdapter.class);

    private AudioPlayerAdapter() {
        System.out.println();
        this.musicManagers = new HashMap<>();

        this.playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerLocalSource(playerManager);
        playerManager.registerSourceManager(new YoutubeAudioSourceManager());
        playerManager.registerSourceManager(new SoundCloudAudioSourceManager());
        playerManager.registerSourceManager(new TwitchStreamAudioSourceManager());
        playerManager.registerSourceManager(new BeamAudioSourceManager());
        playerManager.registerSourceManager(new BandcampAudioSourceManager());
        playerManager.registerSourceManager(new VimeoAudioSourceManager());

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
    }

    public void loadLocalAudio(String filePath, Member member) {
        GuildMusicManager musicManager = getGuildAudioPlayer(member.getGuild());

        playerManager.loadItemOrdered(musicManager, filePath, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack audioTrack) {
                playLocalAudio(member.getGuild(), audioTrack, member);
            }

            @Override
            public void playlistLoaded(AudioPlaylist audioPlaylist) {

            }

            @Override
            public void noMatches() {
                System.out.println("Error. Should not be reached.");
            }

            @Override
            public void loadFailed(FriendlyException e) {
                System.out.println("File not found.");
            }
        });
    }

    /**
     * Using the &add command, load and play a song on the channel.
     * @param channel
     * @param trackUrl
     */
    public void loadAndPlay(final TextChannel channel, final String trackUrl, Member member, boolean writeToChannel) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
        connectToMusicChannel(channel.getGuild().getAudioManager(), member);

        playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                if (writeToChannel) {
                    channel.sendMessage("Adding to queue: ``" + track.getInfo().title + "``.").queue();
                }

                play(channel.getGuild(), musicManager, track, member, channel);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                AudioTrack firstTrack = playlist.getSelectedTrack();

                if (firstTrack == null) {
                    firstTrack = playlist.getTracks().get(0);
                }
                if (writeToChannel) {
                    channel.sendMessage("Adding to queue: ``" + firstTrack.getInfo().title +
                            "`` (first track of playlist ``" + playlist.getName() + "``).").queue();
                }

                play(channel.getGuild(), musicManager, firstTrack, member, channel);
            }

            @Override
            public void noMatches() {
                if (writeToChannel) {
                    channel.sendMessage("Nothing found by ``" + trackUrl + "``").queue();
                }
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                if (writeToChannel) {
                    channel.sendMessage("Could not play: " + exception.getMessage()).queue();
                }
            }
        });
    }

    /**
     * Add the track to the queue.
     * @param guild
     * @param musicManager
     * @param track
     */
    public void play(Guild guild, GuildMusicManager musicManager,
                     AudioTrack track, Member member, MessageChannel msgChan) {
        connectToMusicChannel(guild.getAudioManager(), member);
        musicManager.scheduler.queue(track, member, msgChan);
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
    public static void connectToMusicChannel(AudioManager audioManager, Member member) {
        audioManager.openAudioConnection(member.getVoiceState().getChannel());
    }

    public void playLocalAudio(Guild guild, AudioTrack audioTrack, Member member) {
        // Should be connected already.
        // The handlers should be automatically changed, and the track should be paused, so
        // continue playing it.
        GuildMusicManager manager = getGuildAudioPlayer(guild);
        AudioTrack lastTrack = manager.player.getPlayingTrack();
        if (lastTrack != null) {

//            copyTrack.setMarker(lastTrack.;);
            // Add the copied track as the string, so that when the song is stopped, the
            // voice sample track can start.
            manager.scheduler.storedSongMap.put(lastTrack.getIdentifier(), audioTrack);
//                    new Pair<>(audioTrack, lastTrack.getPosition()));
            manager.player.stopTrack();
        } else {
            manager.player.stopTrack();
            manager.player.playTrack(audioTrack);
        }

    }

}
