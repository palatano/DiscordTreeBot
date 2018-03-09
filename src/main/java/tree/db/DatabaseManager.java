package tree.db;

//import com.mysql.jdbc.Connection;
import javafx.util.Pair;
import net.dv8tion.jda.core.entities.*;
import tree.Config;
import tree.db.EntryInfo;

import javax.swing.text.html.HTMLDocument;
import javax.xml.crypto.Data;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Valued Customer on 9/17/2017.
 */
public class DatabaseManager {
    private static DatabaseManager db = new DatabaseManager();
    private Connection conn;

    private DatabaseManager() {
        try {
            Class.forName("com.mysql.jdbc.Driver");

            String username = Config.getDbUser();
            String password = Config.getDbPassword();

            conn = DriverManager.getConnection("jdbc:mysql://treebot-sql.cxf0hudqqiq9.us-east-2.rds.amazonaws.com:3307/treebot", username, password);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String adminDataToString(String table, String permissionString, ResultSet rs) throws SQLException{

        // Let's do something for admins only.
                                List<EntryInfo> entryList2 = getTableColumnLabels(table, conn);
                        if (table.equals("admins")) {
                            while (rs.next()) {
                                String name = "";
                                String nickName = "";
                                String disc = "";
                                for (EntryInfo entry : entryList2) {
                                    String columnLabel = entry.getColumnLabel();
                                    int type = entry.getType();
                                    if (type == Types.VARCHAR && columnLabel.equals("admin_name")) {
                                        name = rs.getString(columnLabel);
                                    } else if (type == Types.VARCHAR && columnLabel.equals("admin_nickname")) {
                                        nickName = rs.getString(columnLabel);
                                    } else if (type == Types.INTEGER && columnLabel.equals("admin_disc")) {
                                        int discriminator = rs.getInt("admin_disc");
                                        disc = String.valueOf(discriminator);
                    }
                }
                permissionString += nickName == null ?
                        name + ", " :
                        nickName + " (" + name + "#" + disc + "), ";
            }
        }
        permissionString = removeLastComma(permissionString) + "]";
        return permissionString;
    }

    private String tableDataToString(Guild guild, String table) throws SQLException {
        String permissionString = "";
        switch (table) {
            case "text_channels":
                permissionString += "**Allowed Text Channels:** [";
                break;
            case "voice_channels":
                permissionString += "**Allowed Voice Channels:** [";
                break;
            case "admins":
                permissionString += "**Admins:** [";
                break;
            case "music_roles":
                permissionString += "**Authorized Music Roles:** [";
                break;
            default:
                System.out.println("Shouldn't reach here.");
        }

        String query = "SELECT * FROM " + table + " WHERE guild_id = ?";
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setLong(1, guild.getIdLong());
        ResultSet rs = ps.executeQuery();

        if (table.equals("admins")) {
            permissionString = adminDataToString(table, permissionString, rs);
            return permissionString;
        }

        List<EntryInfo> entryList = getTableColumnLabels(table, conn);
        while (rs.next()) {
            for (EntryInfo entry : entryList) {
                String columnLabel = entry.getColumnLabel();
                int type = entry.getType();
                if (type == Types.VARCHAR) {
                    String value = rs.getString(columnLabel);
                    permissionString += value + ", ";
                }
            }
        }
        permissionString = removeLastComma(permissionString) + "]";
        return permissionString;

    }

    public String getGuildPermissions(Guild guild) throws SQLException {
        String output = "Current permissions for " + guild.getName() + ": \n";
        String[] tables = new String[] {"text_channels", "voice_channels", "admins", "music_roles"};
        for (String table : tables) {
            output += tableDataToString(guild, table) + "\n";
        }
        return output;
    }

    public void initializeGuildData(Guild guild) throws SQLException {
        long guildId = guild.getIdLong();
        String name = guild.getName();
        String specifiedColumns = "(guild_id, guild_name)";

        String query = "INSERT INTO " + "guilds " + specifiedColumns +
                " VALUES (?, ?)";
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setLong(1, guildId);
        ps.setString(2, name);
        ps.executeUpdate();
    }

    public void setGuildPermissions(Guild guild, TextChannel channel) throws SQLException {
        long guildId = guild.getIdLong();
        long id = channel.getIdLong();
        String tableName = "text_channels";
        if (checkIfKeyExists(guild, tableName, "text_channel_id", id, false)) {
            return;
        }

        String specifiedColumns = "(text_channel_id, text_channel_name, guild_id)";

        String query = "INSERT INTO " + tableName + " " + specifiedColumns +
                " VALUES (?, ?, ?)";
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setLong(1, channel.getIdLong());
        ps.setString(2, channel.getName());
        ps.setLong(3, guildId);
        ps.executeUpdate();
    }

    public void setGuildPermissions(Guild guild, VoiceChannel channel) throws SQLException {
        long guildId = guild.getIdLong();
        long id = channel.getIdLong();
        String tableName = "voice_channels";
        if (checkIfKeyExists(guild, tableName, "voice_channel_id", id, false)) {
            return;
        }

        String specifiedColumns = "(voice_channel_id, voice_channel_name, guild_id)";

        String query = "INSERT INTO " + tableName + " " + specifiedColumns +
                " VALUES (?, ?, ?)";
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setLong(1, channel.getIdLong());
        ps.setString(2, channel.getName());
        ps.setLong(3, guildId);
        ps.executeUpdate();
    }

    public void setGuildPermissions(Guild guild, Member member) throws SQLException {
        long guildId = guild.getIdLong();
        long adminId = member.getUser().getIdLong();
        String tableName = "admins";
        if (checkIfKeyExists(guild, tableName, "admin_id", adminId, false)) {
            return;
        }

        String specifiedColumns = "(admin_id, admin_name, admin_nickname, admin_disc, guild_id)";

        String query = "INSERT INTO " + tableName + " " + specifiedColumns +
                " VALUES (?, ?, ?, ?, ?)";
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setLong(1, member.getUser().getIdLong());
        ps.setString(2, member.getUser().getName());
        ps.setString(3, member.getNickname());
        ps.setInt(4, Integer.parseInt(member.getUser().getDiscriminator()));
        ps.setLong(5, guildId);
        ps.executeUpdate();
    }

    public void setGuildPermissions(Guild guild, Role role) throws SQLException {
        long guildId = guild.getIdLong();
        long roleId = role.getIdLong();
        String tableName = "music_roles";
        if (checkIfKeyExists(guild, tableName, "music_role_id", roleId, false)) {
            return;
        }

        String specifiedColumns = "(music_role_id, music_role_name, guild_id)";

        String query = "INSERT INTO " + tableName + " " + specifiedColumns +
                " VALUES (?, ?, ?)";
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setLong(1, role.getIdLong());
        ps.setString(2, role.getName());
        ps.setLong(3, guildId);
        ps.executeUpdate();
    }

//    public void setGuildPermissions()

    public static DatabaseManager getInstance() {
        return db;
    }

    private static void getTables(Connection conn) throws SQLException {
        // Let's get the tables.
        DatabaseMetaData md = conn.getMetaData();
        ResultSet rs = md.getTables(null, null, "%", null);
        while (rs.next()) {
            System.out.println(rs.getString(3));
        }
        rs.close();
    }

    private static List<EntryInfo> getTableColumnLabels(String tableName, Connection conn) throws SQLException {
        String query = "SELECT * FROM " + tableName;
        PreparedStatement ps = conn.prepareStatement(query);
        ResultSetMetaData rsmd = ps.getMetaData();

        List<EntryInfo> out = new ArrayList<>();
        int numColumns = rsmd.getColumnCount();
        for (int i = 1; i <= numColumns; i++) {
            String label = rsmd.getColumnLabel(i);
                out.add(new EntryInfo(label, rsmd.getColumnType(i)));

        }
        return out;
    }

    private static String getColumnLabelString(List<EntryInfo> list) {
        String out = "";
        for (EntryInfo entry : list) {
            String label = entry.getColumnLabel();
            out += label + ", ";
        }
        int lastCommaIndex = out.lastIndexOf(",");
        if (lastCommaIndex != -1) {
            return new StringBuilder(out).replace(lastCommaIndex, lastCommaIndex + 2, "").toString();
        }
        return out;
    }

    private void showTableContents(String tableName, String... tables) throws SQLException {

        // Let's get the labels to select from the desired tableName.
        List<EntryInfo> tableLabels = getTableColumnLabels(tableName, conn);
        String tableLabelString = getColumnLabelString(tableLabels);

        // Get the labels from the guild parent table.
        List<EntryInfo> guildLabels = getTableColumnLabels("guilds", conn);
        String guildLabelString = getColumnLabelString(guildLabels);

        // Let's get the data in the tables.
        String query = "SELECT " + tableLabelString + ", " + guildLabelString + "\n" +
                "\tFROM "+ tableName + " JOIN guilds\n" +
                "        ON guilds.guild_id = " + tableName + ".guild_id";
        PreparedStatement ps = conn.prepareStatement(query);
        ResultSet guildData = ps.executeQuery();
        ResultSetMetaData resultSetMetaData = guildData.getMetaData();

        // After getting the metadata, let's iterate over each column.
        int numColumns = resultSetMetaData.getColumnCount();
        List<EntryInfo> columnLabelTypeList = new ArrayList<>();
        for (int i = 1; i <= numColumns; i++) {
            columnLabelTypeList.add(
                    new EntryInfo(resultSetMetaData.getColumnLabel(i), resultSetMetaData.getColumnType(i)));
        }

        // Now, lets get the data.
        int entryIndex = 1;
        System.out.println("\n--- Here is the current data for all " + tableName + "  ---");
        while (guildData.next()) {
            System.out.println("Entry #" + entryIndex++ + ":");
            for (EntryInfo entry : columnLabelTypeList) {
                String columnLabel = entry.getColumnLabel();
                int type = entry.getType();
                if (type == Types.BIGINT) {
                    long value = guildData.getLong(columnLabel);
                    System.out.println(columnLabel + " = " + value);
                } else if (type == Types.INTEGER) {
                    int value = guildData.getInt(columnLabel);
                    System.out.println(columnLabel + " = " + value);
                } else if (type == Types.VARCHAR) {
                    String value = guildData.getString(columnLabel);
                    System.out.println(columnLabel + " = " + value);
                }
            }
        }

    }

    public void removeGuildPermissions(Guild guild, TextChannel channel) throws SQLException {
        String tableName = "text_channels";
        String firstPrimaryKey = "text_channel_id";
        String secondPrimaryKey = "guild_id";

        if (!checkIfKeyExists(guild, tableName, firstPrimaryKey, channel.getIdLong(), false)) {
            return;
        }

        String query = "DELETE FROM " + tableName
                + " WHERE " + firstPrimaryKey + " = ?" +
                " AND " + secondPrimaryKey + " = ?";
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setLong(1, channel.getIdLong());
        ps.setLong(2, guild.getIdLong());
        ps.executeUpdate();
    }

    public void removeGuildPermissions(Guild guild, VoiceChannel channel) throws SQLException {
        String tableName = "voice_channels";
        String firstPrimaryKey = "voice_channel_id";
        String secondPrimaryKey = "guild_id";

        if (!checkIfKeyExists(guild, tableName, firstPrimaryKey, channel.getIdLong(), false)) {
            return;
        }

        String query = "DELETE FROM " + tableName
                + " WHERE " + firstPrimaryKey + " = ?" +
                " AND " + secondPrimaryKey + " = ?";
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setLong(1, channel.getIdLong());
        ps.setLong(2, guild.getIdLong());
        ps.executeUpdate();
    }

    public void removeGuildPermissions(Guild guild, Member member) throws SQLException {
        String tableName = "admins";
        String firstPrimaryKey = "admin_id";
        String secondPrimaryKey = "guild_id";

        if (!checkIfKeyExists(guild, tableName, firstPrimaryKey, member.getUser().getIdLong(), false)) {
            return;
        }

        String query = "DELETE FROM " + tableName
                + " WHERE " + firstPrimaryKey + " = ?" +
                " AND " + secondPrimaryKey + " = ?";
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setLong(1, member.getUser().getIdLong());
        ps.setLong(2, guild.getIdLong());
        ps.executeUpdate();
    }

    public void removeGuildPermissions(Guild guild, Role role) throws SQLException {
        String tableName = "music_roles";
        String firstPrimaryKey = "music_role_id";
        String secondPrimaryKey = "guild_id";
        if (!checkIfKeyExists(guild, tableName, firstPrimaryKey, role.getIdLong(), false)) {
            return;
        }

        String query = "DELETE FROM " + tableName
                + " WHERE " + firstPrimaryKey + " = ?" +
                " AND " + secondPrimaryKey + " = ?";
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setLong(1, role.getIdLong());
        ps.setLong(2, guild.getIdLong());
        ps.executeUpdate();
    }

    public boolean checkIfKeyExists(Guild guild, String tableName, String idLabel,
                                    long id, boolean isGuildTable) throws SQLException {
        PreparedStatement ps;
        if (isGuildTable) {
            ps = conn.prepareStatement("SELECT " + idLabel + " FROM " +
                    tableName + " WHERE " + idLabel + " = ? LIMIT 1");
            ps.setLong(1, id);
        } else {
            ps = conn.prepareStatement("SELECT " + idLabel +
                    " FROM " + tableName +
                    " WHERE " + idLabel + " = ?" +
                    " AND guild_id = ?;");
            ps.setLong(1, id);
            ps.setLong(2, guild.getIdLong());
        }
        ResultSet rs = ps.executeQuery();
        return rs.next();
    }

    public boolean checkIfAllowed(Guild guild, String tableName, String idLabel,
                                    long id, boolean isGuildTable) throws SQLException {
        PreparedStatement ps, psEmpty;
        if (isGuildTable) {
            ps = conn.prepareStatement("SELECT " + idLabel + " FROM " +
                    tableName + " WHERE " + idLabel + " = ? LIMIT 1");
            ps.setLong(1, id);
        } else {
            psEmpty = conn.prepareStatement("SELECT * FROM "
                    + tableName +
                    " WHERE " + "guild_id = ?");
            psEmpty.setLong(1, guild.getIdLong());
            ResultSet rsEmpty = psEmpty.executeQuery();
            if (!rsEmpty.next()) {
                return true;
            }

            ps = conn.prepareStatement("SELECT " + idLabel +
                    " FROM " + tableName +
                    " WHERE " + idLabel + " = ?" +
                    " AND guild_id = ?;");
            ps.setLong(1, id);
            ps.setLong(2, guild.getIdLong());
        }
        ResultSet rs = ps.executeQuery();
        return rs.next();
    }

    private static String removeLastComma(String toChange) {
        int lastCommaIndex = toChange.lastIndexOf(",");
        if (lastCommaIndex != -1) {
            return new StringBuilder(toChange).replace(lastCommaIndex, lastCommaIndex + 2, "").toString();
        }
        return toChange;
    }

    public boolean isAdmin(Guild guild, Member member) {
        try {
            if (!checkIfKeyExists(guild, "guilds", "guild_id", guild.getIdLong(), true)) {
                initializeGuildData(guild);
            }

            return checkIfAllowed(guild, "admins", "admin_id", member.getUser().getIdLong(), false);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isAllowedTextChannel(Guild guild, TextChannel textChannel) {
        try {
            System.out.println("Reached here");
            if (!checkIfKeyExists(guild, "guilds", "guild_id", guild.getIdLong(), true)) {
                System.out.println("Got here");
                initializeGuildData(guild);
            }

            return checkIfAllowed(guild, "text_channels", "text_channel_id", textChannel.getIdLong(), false);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isAllowedVoiceChannel(Guild guild, VoiceChannel voiceChannel) {
        try {
            if (!checkIfKeyExists(guild, "guilds", "guild_id", guild.getIdLong(), true)) {
                initializeGuildData(guild);
            }

            return checkIfAllowed(guild, "voice_channels",
                    "voice_channel_id", voiceChannel.getIdLong(), false);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean hasMusicRole(Guild guild, Member member) {
        PreparedStatement ps, psEmpty = null;
        String tableName = "music_roles";
        String query = "SELECT * FROM  " + tableName + " JOIN guilds" +
                " ON guilds.guild_id = " + tableName + ".guild_id";
        List<Role> roles = member.getRoles();

        if (isAdmin(guild, member)) {
            return true;
        }
        try {
            if (!checkIfKeyExists(guild, "guilds", "guild_id", guild.getIdLong(), true)) {
                initializeGuildData(guild);
            }

            psEmpty = conn.prepareStatement("SELECT * FROM "
                    + tableName +
                    " WHERE " + "guild_id = ?");
            psEmpty.setLong(1, guild.getIdLong());
            ResultSet rsEmpty = psEmpty.executeQuery();
            if (!rsEmpty.next()) {
                return true;
            }

            ps = conn.prepareStatement(query);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                // We want to get the roles listed in the guild.
                long roleId = rs.getLong("music_role_id");
                Role role = guild.getRoleById(roleId);
                if (roles.contains(role)) {
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Role> getMusicRoles(Guild guild) {
        PreparedStatement ps = null;
        String tableName = "music_roles";
        String query = "SELECT * FROM " + tableName + " WHERE guilds.guild_id = " + tableName + "guild.id";
        List<Role> outList = new ArrayList<>();
        try {
            ps = conn.prepareStatement(query);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                long id = rs.getLong("music_role_id");
                Role role = guild.getRoleById(id);
                outList.add(role);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return outList;
    }

}

