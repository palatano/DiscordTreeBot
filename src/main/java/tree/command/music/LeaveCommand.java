package tree.command.music;

import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.managers.AudioManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tree.command.util.music.AudioPlayerAdapter;
import tree.command.util.music.GuildMusicManager;
import tree.commandutil.type.MusicCommand;
import tree.util.LoggerUtil;

import java.util.List;

/**
 * Created by Valued Customer on 8/12/2017.
 */
public class LeaveCommand implements MusicCommand {
    private String commandName;
    private AudioPlayerAdapter audioPlayer;
    private static Logger logger = LoggerFactory.getLogger(LeaveCommand.class);

    public LeaveCommand(String commandName) {
        this.commandName = commandName;
        audioPlayer = AudioPlayerAdapter.audioPlayerAdapter;
    }

    private void leave(Guild guild, MessageChannel msgChan, Message message, Member member) {
        GuildMusicManager musicManager = AudioPlayerAdapter.audioPlayerAdapter.getGuildAudioPlayer(guild);
        musicManager.player.setPaused(true);
        guild.getAudioManager().closeAudioConnection();
    }

    @Override
    public void execute(Guild guild, MessageChannel msgChan, Message message, Member member, String[] args) {
        if (args.length != 1) {
            LoggerUtil.logMessage(logger, message, "Only one argument allowed.");
            return;
        }
        leave(guild, msgChan, message, member);
    }

    @Override
    public String help() {
        return "Leave the music channel.";
    }

    @Override
    public String getCommandName() {
        return commandName;
    }

}
