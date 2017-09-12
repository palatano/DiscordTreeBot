package tree.command.music;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import tree.command.util.music.AudioPlayerAdapter;
import tree.command.util.music.GuildMusicManager;
import tree.commandutil.CommandManager;
import tree.commandutil.type.MusicCommand;

/**
 * Created by Valued Customer on 8/15/2017.
 */
public class UndoCommand implements MusicCommand {
    private String commandName;

    public UndoCommand(String commandName) {
        this.commandName = commandName;
    }

    @Override
    public void execute(Guild guild, MessageChannel msgChan, Message message, Member member, String[] args) {
        GuildMusicManager musicManager = AudioPlayerAdapter.audioPlayerAdapter
                .getGuildAudioPlayer(guild);
        musicManager.scheduler.removeLastTrack(guild, msgChan, message);
    }

    @Override
    public String help() {
        return "``" + CommandManager.botToken + commandName + "``: Undo the last song added.";
    }

    @Override
    public String getCommandName() {
        return commandName;
    }
}
