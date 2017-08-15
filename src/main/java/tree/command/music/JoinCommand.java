package tree.command.music;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.managers.AudioManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tree.command.util.music.AudioPlayerAdapter;
import tree.command.util.music.GuildMusicManager;
import tree.commandutil.type.MusicCommand;
import tree.util.LoggerUtil;

/**
 * Created by Valued Customer on 8/12/2017.
 */
public class JoinCommand implements MusicCommand {
    private String commandName;
    private AudioPlayerAdapter audioPlayer;
    private static Logger logger = LoggerFactory.getLogger(LeaveCommand.class);

    public JoinCommand(String commandName) {
        this.commandName = commandName;
        audioPlayer = AudioPlayerAdapter.audioPlayer;
    }

    private void join(Guild guild, MessageChannel msgChan, Message message, Member member) {
        AudioManager audioManager = guild.getAudioManager();
        GuildMusicManager musicManager = AudioPlayerAdapter.audioPlayer.getGuildAudioPlayer(guild);
        if (guild.getAudioManager().isConnected() || guild.getAudioManager().isAttemptingToConnect()) {
            return;
        }
        audioPlayer.connectToMusicChannel(guild.getAudioManager());
    }

    @Override
    public void execute(Guild guild, MessageChannel msgChan, Message message, Member member, String[] args) {
        if (args.length != 1) {
            LoggerUtil.logMessage(logger, message, "Only one argument allowed.");
            return;
        }
        join(guild, msgChan, message, member);
    }

    @Override
    public String help() {
        return "Join the music channel.";
    }

    @Override
    public String getCommandName() {
        return commandName;
    }


}
