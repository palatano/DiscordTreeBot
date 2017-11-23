package tree.command.analysis;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import tree.Config;
import tree.commandutil.CommandManager;
import tree.commandutil.type.AnalysisCommand;
import tree.db.DatabaseManager;

import java.sql.SQLException;

/**
 * Created by Valued Customer on 8/31/2017.
 */
public class GuildPermissionsCommand implements AnalysisCommand {
    private String commandName;
    private DatabaseManager db;

    public GuildPermissionsCommand(String commandName) {
        this.commandName = commandName;
        db = DatabaseManager.getInstance();
    }

    @Override
    public void execute(Guild guild, MessageChannel msgChan, Message message, Member member, String[] args) {
        String output = "";
        try {
            output = db.getGuildPermissions(guild);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        msgChan.sendMessage(output).queue();
    }

    @Override
    public String help() {
        return "``" + CommandManager.botToken + commandName + "``: Lists the current permissions in the guild.";
    }

    @Override
    public String getCommandName() {
        return commandName;
    }
}
