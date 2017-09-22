package tree.command.data.permsdata;

import net.dv8tion.jda.core.entities.Guild;

import java.sql.SQLException;

/**
 * Created by Valued Customer on 9/21/2017.
 */
public class TextChannelPerm extends GuildPerm {
//    private TextChannel channel;
//
//    public TextChannel getChannel() {
//        return channel;
//    }
//
//    public TextChannelPerm(TextChannel channel) {
//        this.channel = channel;
//    }

    @Override
    public void accept(Guild guild, GuildPermissionsVisitor visitor, long id) throws SQLException {
        visitor.visit(guild, this, id);
    }
}
