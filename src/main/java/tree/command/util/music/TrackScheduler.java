package tree.command.util.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.*;
import javafx.util.Pair;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import tree.command.util.MessageUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * This class schedules tracks for the audio player. It contains the queue of tracks.
 */
public class TrackScheduler extends AudioEventAdapter {
    private final AudioPlayer player;
    private final BlockingQueue<AudioTrack> queue;
    private Map<AudioTrack, Member> personAddedMap;
    private Map<Guild, AudioTrack> lastSongAddedMap;
    public Map<String, AudioTrack> storedSongMap;
    private static final int MAX_SONGS_LISTED = 7;


    public void removeLastTrack(Guild guild, MessageChannel msgChan, Message message) {
        AudioTrack track = lastSongAddedMap.get(guild);
        if (track == null) {
            message.addReaction("\u274E").queue();
            return;
        }
        if (track.equals(player.getPlayingTrack())) {
            player.stopTrack();
            lastSongAddedMap.remove(guild);
            message.addReaction("\u2705").queue();
            return;
        }
        lastSongAddedMap.remove(guild);
        queue.remove(track);
        message.addReaction("\u2705").queue();
    }

    public boolean isEmpty() {
        return queue.isEmpty() && player.getPlayingTrack() == null;
    }

    /**
     * @param player The audio player this scheduler uses
     */
    public TrackScheduler(AudioPlayer player) {
        this.player = player;
        this.queue = new LinkedBlockingQueue<>();
        this.personAddedMap = new HashMap<>();
        this.lastSongAddedMap = new HashMap<>();
        this.storedSongMap = new HashMap<>();
    }

    /**
     * Add the next track to queue or play right away if nothing is in the queue.
     *
     * @param track The track to play or add to queue.
     */
    public void queue(AudioTrack track, Member member) {
        // Calling startTrack with the noInterrupt set to true will start the track only if nothing is currently playing. If
        // something is playing, it returns false and does nothing. In that case the player was already playing so this
        // track goes to the queue instead.
        personAddedMap.put(track, member);
        lastSongAddedMap.put(member.getGuild(), track);
        if (!player.startTrack(track, true)) {
            queue.offer(track);
        }
    }

    /**
     * Start the next track, stopping the current one if it is playing.
     */
    public void nextTrack() {
        // Start the next track, regardless of if something is already playing or not. In case queue was empty, we are
        // giving null to startTrack, which is a valid argument and will simply stop the player.
        player.startTrack(queue.poll(), false);
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        // Only start the next track if the end reason is suitable for it (FINISHED or LOAD_FAILED)

        String name = track.getIdentifier();
        // The track that just stopped is track, and the localTrack is the voice search file.
        if (storedSongMap.containsKey(name)) {
            if (name.contains("out.wav")) {
                    AudioTrack stoppedTrack = storedSongMap.get(name);
                    player.stopTrack();
                    player.playTrack(stoppedTrack);
            } else {
                // Start the voice search file.
                AudioTrack voiceTrack = storedSongMap.get(name);
                AudioTrack copyTrack = track.makeClone();
                copyTrack.setPosition(track.getPosition());
                copyTrack.setUserData(voiceTrack.getUserData());
                storedSongMap.remove(track.getIdentifier());
                storedSongMap.put(voiceTrack.getIdentifier(), copyTrack);
                player.playTrack(voiceTrack);
            }
            return;
        }

        if (endReason.mayStartNext) {
            nextTrack();
        }
    }

    private String getSongDuration(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        String hoursString = (hours / 10L) == 0 ? "0" + hours : String.valueOf(hours);
        String minutesString = (minutes / 10L) == 0 ? "0" + minutes : String.valueOf(minutes);
        String secondsString = (seconds / 10L) == 0 ? "0" + seconds : String.valueOf(seconds);
        if (hours > 0 && hours < 2) {
            return "(" + hoursString + ":" + minutesString + ":" + secondsString + ")";
        } else {
            return "(" +  minutesString + ":" + secondsString + ")";
        }
    }

    public String printSongList() {
        String list = "";
        int counter = 0;
        // For first song:
        if (player.getPlayingTrack() == null) {
            return "No songs are currently playing.";
        }
        Member currMember = personAddedMap.get(player.getPlayingTrack());
        AudioTrackInfo currentSongInfo = player.getPlayingTrack().getInfo();
        list = "``Now Playing:`` **" + currentSongInfo.title + "** " + getSongDuration(currentSongInfo.length) +
                ", added by ``" + currMember.getEffectiveName() + "``\n\n";

        for (AudioTrack track : queue) {
            AudioTrackInfo info = track.getInfo();
            Member member = personAddedMap.get(track);
            list += "``Song " + ++counter + ")`` **" + info.title + "** " +
                    getSongDuration(info.length) + " - added by ``" + member.getEffectiveName() + "``\n";
            if (counter == MAX_SONGS_LISTED) {
                break;
            }
        }
        list += "\n**Total number of songs: " + (queue.size() + 1) +
                "**. Set the automatic playlist feature with" +
                " ``;list on`` or ``list off``.";
        return list;
    }

    public String showCurrentSong() {
        String list = "";
        if (player.getPlayingTrack() == null) {
            return "No songs are currently playing.";
        }
        Member currMember = personAddedMap.get(player.getPlayingTrack());
        AudioTrackInfo currentSongInfo = player.getPlayingTrack().getInfo();
        list += "``Now Playing:`` **" + currentSongInfo.title + "** " + getSongDuration(currentSongInfo.length) +
                ", added by ``" + currMember.getEffectiveName() + "``\n\n";
        if (!queue.isEmpty()) {
            AudioTrack nextTrack = queue.peek();
            Member nextMember = personAddedMap.get(nextTrack);
            AudioTrackInfo nextTrackInfo = nextTrack.getInfo();
            list += "``Next Song``: **" + nextTrackInfo.title + "** " + getSongDuration(nextTrackInfo.length) +
                    ", added by ``" + nextMember.getEffectiveName() + "``\n";
        }
        return list;
    }

}
