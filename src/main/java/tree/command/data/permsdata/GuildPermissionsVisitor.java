package tree.command.data.permsdata;

import net.dv8tion.jda.core.entities.*;
import tree.command.data.permsdata.MemberPerm;
import tree.command.data.permsdata.MusicRolePerm;
import tree.command.data.permsdata.TextChannelPerm;
import tree.command.data.permsdata.VoiceChannelPerm;

import java.sql.SQLException;

/**
 * Created by Valued Customer on 9/21/2017.
 */
public interface GuildPermissionsVisitor {
    void visit(Guild guild, TextChannelPerm channel, long id) throws SQLException;
    void visit(Guild guild, VoiceChannelPerm channel, long id) throws SQLException;
    void visit(Guild guild, MemberPerm member, long id) throws SQLException;
    void visit(Guild guild, MusicRolePerm role, long id) throws SQLException;
}
