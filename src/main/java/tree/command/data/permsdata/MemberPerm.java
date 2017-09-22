package tree.command.data.permsdata;

import net.dv8tion.jda.core.entities.Guild;

import java.sql.SQLException;

/**
 * Created by Valued Customer on 9/21/2017.
 */
public class MemberPerm extends GuildPerm {
//    private Member member;
//
//    public Member getMember() {
//        return member;
//    }
//
//    public MemberPerm(Member member) {
//        this.member = member;
//    }

    @Override
    public void accept(Guild guild, GuildPermissionsVisitor visitor, long id) throws SQLException {
        visitor.visit(guild, this, id);
    }
}
