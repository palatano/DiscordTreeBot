package tree.command.music;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tree.command.util.music.AudioPlayerAdapter;
import tree.command.util.music.GuildMusicManager;
import tree.commandutil.CommandManager;
import tree.commandutil.type.MusicCommand;
import tree.util.LoggerUtil;

/**
 * Created by Valued Customer on 8/12/2017.
 */
public class UnpauseCommand implements MusicCommand {
    private String commandName;
    private AudioPlayerAdapter audioPlayer;
    private static Logger logger = LoggerFactory.getLogger(AddCommand.class);

    public UnpauseCommand(String commandName) {
        this.commandName = commandName;
        audioPlayer = AudioPlayerAdapter.audioPlayerAdapter;
    }

    private void resume(Guild guild, MessageChannel msgChan, Message message, Member member) {
        GuildMusicManager musicManager = AudioPlayerAdapter.audioPlayerAdapter.getGuildAudioPlayer(guild);
        msgChan.sendMessage("Song will now continue.").queue();
        musicManager.player.setPaused(false);
    }

    @Override
    public void execute(Guild guild, MessageChannel msgChan, Message message, Member member, String[] args) {
        if (args.length != 1) {
            LoggerUtil.logMessage(logger, message, "Only one argument allowed.");
            return;
        }
        resume(guild, msgChan, message, member);
    }

    @Override
    public String help() {
        return "``" + CommandManager.botToken + commandName + "``: Unpause the player.";
    }

    @Override
    public String getCommandName() {
        return commandName;
    }
}
