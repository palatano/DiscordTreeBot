package tree.command.data.permsdata;

import net.dv8tion.jda.core.entities.*;
import tree.db.DatabaseManager;

import java.sql.SQLException;

/**
 * Created by Valued Customer on 9/22/2017.
 */
public class UnsetPermissionsVisitor implements GuildPermissionsVisitor{
    private DatabaseManager db = DatabaseManager.getInstance();

    public void visit(Guild guild, TextChannelPerm textPerm, long id) throws SQLException {
        TextChannel textChannel = guild.getTextChannelById(id);
        db.removeGuildPermissions(guild, textChannel);
    }

    public void visit(Guild guild, VoiceChannelPerm voicePerm, long id) throws SQLException {
        VoiceChannel voiceChannel = guild.getVoiceChannelById(id);
        db.removeGuildPermissions(guild, voiceChannel);
    }

    public void visit(Guild guild, MemberPerm memberPerm, long id) throws SQLException {
        Member member = guild.getMemberById(id);
        db.removeGuildPermissions(guild, member);
    }

    public void visit(Guild guild, MusicRolePerm rolePerm, long id) throws SQLException {
        Role role = guild.getRoleById(id);
        db.removeGuildPermissions(guild, role);
    }

}
