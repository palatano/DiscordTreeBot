package tree.command.data.permsdata;

import net.dv8tion.jda.core.entities.Guild;
import tree.command.data.permsdata.GuildPermissionsVisitor;

import java.sql.SQLException;

/**
 * Created by Valued Customer on 9/21/2017.
 */
public abstract class GuildPerm {
    public abstract void accept(Guild guild, GuildPermissionsVisitor visitor, long id) throws SQLException;
}
