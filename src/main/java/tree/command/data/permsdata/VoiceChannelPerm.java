package tree.command.data.permsdata;

import net.dv8tion.jda.core.entities.Guild;

import java.sql.SQLException;

/**
 * Created by Valued Customer on 9/21/2017.
 */
public class VoiceChannelPerm extends GuildPerm {
//    private VoiceChannel voiceChannel;
//
//    public VoiceChannel getVoiceChannel() {
//        return voiceChannel;
//    }
//
//    public VoiceChannelPerm(VoiceChannel voiceChannel) {
//        this.voiceChannel = voiceChannel;
//    }


    @Override
    public void accept(Guild guild, GuildPermissionsVisitor visitor, long id) throws SQLException {
        visitor.visit(guild, this, id);
    }
}
