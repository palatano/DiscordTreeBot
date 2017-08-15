package tree.command.music;

import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.managers.AudioManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tree.command.util.music.AudioPlayerAdapter;
import tree.commandutil.type.MusicCommand;
import tree.util.LoggerUtil;

import java.util.List;

/**
 * Created by Valued Customer on 8/12/2017.
 */
public class SkipCommand implements MusicCommand {
    private String commandName;
    private AudioPlayerAdapter audioPlayer;
    private static Logger logger = LoggerFactory.getLogger(AddCommand.class);

    public SkipCommand(String commandName) {
        this.commandName = commandName;
        audioPlayer = AudioPlayerAdapter.audioPlayer;
    }

    private boolean connect(Guild guild, MessageChannel msgChan, Message message, Member member) {
        List<VoiceChannel> voiceChannelList = guild.getVoiceChannelsByName("music", true);
        AudioManager audioManager = guild.getAudioManager();
        if (voiceChannelList.isEmpty()) {
            LoggerUtil.logMessage(logger, message, "No #music channel found.");
            return false;
        }
        audioManager.openAudioConnection(voiceChannelList.get(0));
        return true;
    }

    private void skipSong(Guild guild, MessageChannel msgChan, Message message, Member member) {
        audioPlayer.skipTrack(message.getTextChannel());
    }

    @Override
    public void execute(Guild guild, MessageChannel msgChan, Message message, Member member, String[] args) {
        if (args.length != 1) {
            LoggerUtil.logMessage(logger, message, "Only one argument allowed.");
        }
        skipSong(guild, msgChan, message, member);
    }

    @Override
    public String help() {
        return "Skip the current song.";
    }

    @Override
    public String getCommandName() {
        return commandName;
    }
}
