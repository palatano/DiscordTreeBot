package tree.command.util.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.*;
import javafx.util.Pair;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.poi.hssf.util.HSSFColor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import tree.command.util.MessageUtil;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
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
    private static final String BLACK_BAR = "\u25ac";
    private static final String PLAY_BAR = "\ud83d\udd18";
    private static final int BAR_LENGTH = 20;

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

    public String printSongList() {
        String list = "";
        int counter = 0;

        // For first song:
        AudioTrack currTrack = player.getPlayingTrack();
        if (currTrack == null) {
            return "No songs are currently playing.";
        }
        Member currMember = personAddedMap.get(currTrack);
        AudioTrackInfo currentSongInfo = currTrack.getInfo();
        long pos = currTrack.getPosition();
        long total = currTrack.getDuration();
        boolean addHour = new Timestamp(total).toLocalDateTime().getHour() - 19 > 0;
        String duration = "``(" + getTimeStamp(pos, addHour) + "/" +
                getTimeStamp(total, addHour) + ")``";

        list = "``Now Playing:`` **" + currentSongInfo.title + "** " + duration +
                ", added by ``" + currMember.getEffectiveName() + "``\n\n";

        for (AudioTrack track : queue) {
            AudioTrackInfo info = track.getInfo();
            Member member = personAddedMap.get(track);
            long totalIter = currTrack.getDuration();
            boolean addHourIter = new Timestamp(totalIter).toLocalDateTime().getHour() - 19 > 0;
            String durationIter = "``(" + getTimeStamp(total, addHourIter) + ")``";

            list += "``Song " + ++counter + ")`` **" + info.title + "** " +
                    durationIter + " - added by ``" + member.getEffectiveName() + "``\n";
            if (counter == MAX_SONGS_LISTED) {
                break;
            }
        }
        list += "\n**Total number of songs: " + (queue.size() + 1) +
                "**. Set the automatic playlist feature with" +
                " ``;list on`` or ``list off``.";
        return list;
    }

    public MessageEmbed showCurrentSong() {
        String list = "";
        EmbedBuilder embed = new EmbedBuilder();
        AudioTrack currTrack = player.getPlayingTrack();
        if (currTrack == null) {
            return embed.appendDescription("No songs are currently playing.").build();
        }

        getLyricsURL(currTrack.getInfo().title);
        Member currMember = personAddedMap.get(currTrack);
        AudioTrackInfo currentSongInfo = currTrack.getInfo();
        list += currentSongInfo.title;

        String nextSong = "";
        if (!queue.isEmpty()) {
            AudioTrack nextTrack = queue.peek();
            Member nextMember = personAddedMap.get(nextTrack);
            AudioTrackInfo nextTrackInfo = nextTrack.getInfo();
            long total = nextTrack.getDuration();
            boolean addHourNext = new Timestamp(total).toLocalDateTime().getHour() - 19 > 0;
            String durationNext = "(" + getTimeStamp(total, addHourNext) + ")";
            nextSong += "Next Song: " + nextTrackInfo.title + " - " + durationNext +
                    ", added by " + nextMember.getEffectiveName() + "\n";
        }

        embed.addField("[" + list + "](" + "https://images-ext-1.discordapp.net/external/OPzZxwonjTBq3WLx3xaG2Drro3y4eGYkNAt4-rnSJAA/https/cdn.discordapp.com/avatars/337627312830939136/a992a2003c85ae69a5bf5d3fe875460a.png?width=80&height=80", createBar(currTrack), true);
        embed.setFooter(nextSong, "https://images-ext-1.discordapp.net/external/OPzZxwonjTBq3WLx3xaG2Drro3y4eGYkNAt4-rnSJAA/https/cdn.discordapp.com/avatars/337627312830939136/a992a2003c85ae69a5bf5d3fe875460a.png?width=80&height=80");
        return embed.build();
    }

    private String getTimeStamp(long val, boolean addHour) {
        Timestamp stamp = new Timestamp(val);
        // For some reason, the default returned from youtube is 1969-12-31 19:00:00.0, so
        // subtract 19 from the hours to get adjusted time.
        int hour = stamp.toLocalDateTime().getHour() - 19;
        String hourString = hour < 10 ? "0" + hour : String.valueOf(hour);
        int minute = stamp.toLocalDateTime().getMinute();
        String minuteString = minute < 10 ? "0" + minute : String.valueOf(minute);
        int second = stamp.toLocalDateTime().getSecond();
        String secondString = second < 10 ? "0" + second : String.valueOf(second);

        return (hour == 0 && !addHour) ? minuteString + ":" + secondString :
                hourString + ":" + minuteString + ":" + secondString;
    }

    private String createBar(AudioTrack track) {
        StringBuilder bar = new StringBuilder();
        long pos = track.getPosition();
        long total = track.getDuration();
        int calculatedPos = (int) Math.floor ((pos * BAR_LENGTH) / total);
        for (int i = 0; i < BAR_LENGTH; i++) {
            if (i == calculatedPos) {
                bar.append(PLAY_BAR);
            } else {
                bar.append(BLACK_BAR);
            }
        }
        boolean addHour = new Timestamp(total).toLocalDateTime().getHour() - 19 > 0;
        String duration = "``[" + getTimeStamp(pos, addHour) + "/" +
                getTimeStamp(total, addHour) + "]``";
        bar.append("  " + duration);
        return bar.toString();
    }

    public String getLyricsURL(String search) {
        URI url = null;
        try {
            url = new URIBuilder("https://genius.com/search")
                    .addParameter("q", search)
                    .build();
            Document links2 = Jsoup.connect(url.toString())
                    .userAgent("TreeBot")
                    .get();
            Elements links = Jsoup.connect(url.toString())
                    .userAgent("TreeBot")
                    .get()
                    .select(".g");

            Element result2 = links.select(".mini-card").first();
//                    .select("a.mini-card");
            if (links.isEmpty()) {
                return null;
            }
            for (Element link : links) {
                String title = link.text();
                String resultUrl = link.absUrl("href")
                        .replace(")", "\\)");
                System.out.println(resultUrl);
                if (!resultUrl.startsWith("http")) {
                    continue;
                }
                return resultUrl;
            }

        } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
        }

        return null;
    }

}
