package tree.command.data.permsdata;

import net.dv8tion.jda.core.entities.Guild;

import java.sql.SQLException;

/**
 * Created by Valued Customer on 9/21/2017.
 */
public class MusicRolePerm extends GuildPerm {
//    private Role role;
//
//    public Role getRole() {
//        return role;
//    }
//
//    public MusicRolePerm(Role role) {
//        this.role = role;
//    }

    @Override
    public void accept(Guild guild, GuildPermissionsVisitor visitor, long id) throws SQLException {
        visitor.visit(guild, this, id);
    }
}
