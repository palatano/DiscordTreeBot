package tree;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.Role;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import tree.db.JDBCInit;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

import static tree.Config.SetType.*;

/**
 * Created by Admin on 7/29/2017.
 */
public class Config {
    public static Config CONFIG;
    private static String botToken;
    private static String filePath;
    private static String osName;
    private static long owner;
    private static String youtubeAPIKey;
    private static boolean isTesting = false;
    private static String guildDataFilepath;
    public static Map<Long, String> guildFilePaths;
    public static Map<Long, Set<Long>> guildAllowedTextChannels;
    public static Map<Long, Set<Long>> guildAllowedVoiceChannels;
    public static Map<Long, Set<Long>> guildAdmins;
    public static long startTime;

    public static Map<Long, Set<Long>> getGuildAllowedTextChannels() {
        return guildAllowedTextChannels;
    }

    public static Map<Long, Set<Long>> getGuildAllowedVoiceChannels() {
        return guildAllowedVoiceChannels;
    }

    public static Map<Long, Set<Long>> getGuildAdmins() {
        return guildAdmins;
    }

    public static Map<Long, Set<Long>> getGuildMusicRoles() {
        return guildMusicRoles;
    }

    public static Map<Long, Set<Long>> guildMusicRoles;

    public static boolean isOwner(long id) {
        return id == owner;
    }

    public static boolean hasMusicRole(Guild guild, Member member) {

        long guildId = guild.getIdLong();
        if (!guildMusicRoles.containsKey(guildId)) {
            createNewGuildData(guild);
        }

        Set<Long> roles = guildMusicRoles.get(guildId);
        if (roles.isEmpty() || isAdmin(guild, member)) {
            return true;
        }

        for (Role role : member.getRoles()) {
            long roleId = role.getIdLong();
            if (roles.contains(roleId)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAdmin(Guild guild, Member member) {
        long guildId = guild.getIdLong();

        if (!guildAdmins.containsKey(guildId)) {
            createNewGuildData(guild);
        }

        if (member.isOwner() || isOwner(member.getUser().getIdLong())) {
            return true;
        }

        Set<Long> admins = guildAdmins.get(guildId);
        return admins.contains(member.getUser().getIdLong());

    }

    public static boolean isAllowedTextChannel(Guild guild, long chanId) {
        if (!guildMusicRoles.containsKey(guild.getIdLong())) {
            createNewGuildData(guild);
        }

        Set<Long> textChannels = guildAllowedTextChannels.get(guild.getIdLong());

        if (textChannels == null || textChannels.isEmpty()) {
            return true;
        }

        return textChannels.contains(chanId);
    }

    public static boolean isAllowedVoiceChannel(Guild guild, long chanId) {
        if (!guildMusicRoles.containsKey(guild.getIdLong())) {
            createNewGuildData(guild);
        }

        Set<Long> voiceChannels = guildAllowedVoiceChannels.get(guild.getIdLong());
        if (voiceChannels == null || voiceChannels.isEmpty()) {
            return true;
        }

        return voiceChannels.contains(chanId);
    }

    public enum SetType {
        TEXT_CHANNEL("textChannels"), VOICE_CHANNEL("voiceChannels"),
        ADMIN("admins"), MUSIC_ROLE("musicRoles");

        private String type;

        SetType(String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return type;
        }
    }

    public static String getPermissions(Guild guild) {
        if (!guildFilePaths.containsKey(guild.getIdLong())) {
            createNewGuildData(guild);
        }
        long guildId = guild.getIdLong();
        String guildName = guild.getName().replaceAll("³", "'");
        String output = "Current permissions for " + guildName + ":\n";

        output += "**Allowed Text Channels:** [";
        int lastIdx;
        if (!guildAllowedTextChannels.get(guildId).isEmpty()) {
            for (long textId : guildAllowedTextChannels.get(guildId)) {
                output += guild.getTextChannelById(textId).getName() + ", ";
            }
            lastIdx = output.lastIndexOf(",");
            output = new StringBuilder(output).replace(lastIdx, lastIdx + 2, "]\n").toString();
        } else {
            output += "]\n";
        }

        output += "**Allowed Voice Channels:** [";
        if (!guildAllowedVoiceChannels.get(guildId).isEmpty()) {
            for (long voiceId : guildAllowedVoiceChannels.get(guildId)) {
                output += guild.getVoiceChannelById(voiceId).getName() + ", ";
            }
            lastIdx = output.lastIndexOf(",");
            output = new StringBuilder(output).replace(lastIdx, lastIdx + 2, "]\n").toString();
        } else {
            output += "]\n";
        }

        output += "**Admins:** [";
        if (!guildAdmins.get(guildId).isEmpty()) {
            for (long adminId : guildAdmins.get(guildId)) {
                output += guild.getMemberById(adminId).getEffectiveName() + ", ";
            }
            lastIdx = output.lastIndexOf(",");
            output = new StringBuilder(output).replace(lastIdx, lastIdx + 2, "]\n").toString();
        } else {
            output += "]\n";
        }

        output += "**Permitted Music Roles:** [";
        if (!guildMusicRoles.get(guildId).isEmpty()) {
            for (long roleId : guildMusicRoles.get(guildId)) {
                output += guild.getRoleById(roleId).getName() + ", ";
            }
            lastIdx = output.lastIndexOf(",");
            output = new StringBuilder(output).replace(lastIdx, lastIdx + 2, "]\n").toString();
        } else {
            output += "]\n";
        }
        return output;
    }

    private static void createNewGuildData(Guild guild) {
        if (guildFilePaths.containsKey(guild.getIdLong())) {
            return;
        }
        long guildId = guild.getIdLong();
        String name = guild.getId() + ".yaml";
        guildFilePaths.put(guild.getIdLong(), guildDataFilepath + name);
        File newGuildFile = new File(guildDataFilepath + name);
        String guildName = guild.getName().replaceAll("'", "³");
        try {
            FileUtils.writeStringToFile(newGuildFile, "name: '" + guildName + "'" + "\n", false);
            FileUtils.writeStringToFile(newGuildFile, "textChannels: ''" + "\n", true);
            FileUtils.writeStringToFile(newGuildFile, "voiceChannels: ''" + "\n", true);
            FileUtils.writeStringToFile(newGuildFile, "admins: ''" + "\n", true);
            FileUtils.writeStringToFile(newGuildFile, "musicRoles: ''" + "\n", true);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        guildAllowedTextChannels.put(guildId, new HashSet<>());
        guildAllowedVoiceChannels.put(guildId, new HashSet<>());
        guildAdmins.put(guildId, new HashSet<>());
        guildMusicRoles.put(guildId, new HashSet<>());
    }

    private static void fileAddition(SetType type, Guild guild,
                              Set<Long> setToUpdate) {
        long guildId = guild.getIdLong();
        if (!guildFilePaths.containsKey(guildId)) {
            createNewGuildData(guild);
        }
        String filePath = guildFilePaths.get(guildId);
        Yaml yaml = new Yaml();
        String fileString;
        FileWriter writer;
        Map<String, Object> guildConfigMap;
        try {
            fileString = FileUtils.readFileToString(new File(filePath));

            // If quotations exist in the name.
            int firstIndex = fileString.indexOf(":");
            int newLineIndex = fileString.indexOf("\n");
            String substring = fileString.substring(firstIndex, newLineIndex);
            String substringReplacement = substring.replaceAll("'", "³");
            fileString = new StringBuilder(fileString)
                    .replace(firstIndex, newLineIndex, substringReplacement).toString();

            guildConfigMap =
                    (Map<String, Object>) yaml.load(fileString);
            writer = new FileWriter(filePath, false);
        } catch (IOException e) {
            System.out.println("Something went wrong with setting the guild setting");
            return;
        }
        guildConfigMap.put(type.toString(), setToUpdate.toString());
        String output = "";
        for (Map.Entry<String, Object> entry : guildConfigMap.entrySet()) {
            String field = entry.getKey();
            String value = (String) entry.getValue();
            if (value.equals("[]")) {
                value = "";
            }
            output += field + ": '" + value + "'\n";
        }
        try {
            writer.write(output);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void fileDeletion(SetType type, Guild guild,
                              Set<Long> setToUpdate) {
        long guildId = guild.getIdLong();
        if (!guildFilePaths.containsKey(guildId)) {
            createNewGuildData(guild);
            return;
        }

        String filePath = guildFilePaths.get(guildId);
        Yaml yaml = new Yaml();
        String fileString;
        FileWriter writer;
        Map<String, Object> guildConfigMap;
        try {
            fileString = FileUtils.readFileToString(new File(filePath));
            int firstIndex = fileString.indexOf(":");
            int newLineIndex = fileString.indexOf("\n");
            String substring = fileString.substring(firstIndex, newLineIndex);
            String substringReplacement = substring.replaceAll("'", "³");
            fileString = new StringBuilder(fileString)
                    .replace(firstIndex, newLineIndex, substringReplacement).toString();
            guildConfigMap =
                    (Map<String, Object>) yaml.load(fileString);
            writer = new FileWriter(filePath, false);
        } catch (IOException e) {
            System.out.println("Something went wrong with setting the guild setting");
            return;
        }
        guildConfigMap.put(type.toString(), setToUpdate.toString());
        String output = "";
        for (Map.Entry<String, Object> entry : guildConfigMap.entrySet()) {
            String field = entry.getKey();
            String value = (String) entry.getValue();
            if (value.equals("[]")) {
                value = "";
            }
            output += field + ": '" + value + "'\n";
        }
        try {
            writer.write(output);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }    }

    public static void setGuildInfo(SetType type, Guild guild,
                                    long setId) {
        long guildId = guild.getIdLong();
        switch (type) {
            case TEXT_CHANNEL:
                guildAllowedTextChannels.get(guildId).add(setId);
                fileAddition(type, guild,
                        guildAllowedTextChannels.get(guildId));
                break;
            case VOICE_CHANNEL:
                guildAllowedVoiceChannels.get(guildId).add(setId);
                fileAddition(type, guild,
                        guildAllowedVoiceChannels.get(guildId));
                break;
            case ADMIN:
                guildAdmins.get(guildId).add(setId);
                fileAddition(type, guild,
                        guildAdmins.get(guildId));
                break;
            case MUSIC_ROLE:
                guildMusicRoles.get(guildId).add(setId);
                fileAddition(type, guild,
                        guildMusicRoles.get(guildId));
                break;
            default:
                System.out.println("Should not reach here.");
        }
    }

    public static void removeGuildInfo(SetType type, Guild guild,
                                       long setId) {
        long guildId = guild.getIdLong();
        switch (type) {
            case TEXT_CHANNEL:
                guildAllowedTextChannels.get(guildId).remove(setId);
                fileDeletion(type, guild,
                        guildAllowedTextChannels.get(guildId));
                break;
            case VOICE_CHANNEL:
                guildAllowedVoiceChannels.get(guildId).remove(setId);
                fileDeletion(type, guild,
                        guildAllowedVoiceChannels.get(guildId));
                break;
            case ADMIN:
                guildAdmins.get(guildId).remove(setId);
                fileDeletion(type, guild,
                        guildAdmins.get(guildId));
                break;
            case MUSIC_ROLE:
                guildMusicRoles.get(guildId).remove(setId);
                fileDeletion(type, guild,
                        guildMusicRoles.get(guildId));
                break;
            default:
                System.out.println("Should not reach here.");
        }
    }

    private void initializeGuildData(File guildFile, Map<String, Object> creds) {
        Yaml guildYaml = new Yaml();
        File guildConfigFile = guildFile;
        long guildId = Long.parseLong(guildFile.getName().replaceFirst(".yaml", ""));
        guildFilePaths.put(guildId, guildFile.getAbsolutePath());
        String guildConfigString = null;
        try {
            guildConfigString = FileUtils.readFileToString(guildFile, "UTF-8");
        } catch (IOException e) {
            System.out.println("Something happened with the initialization of " + guildFile.getName());
            return;
        }
        Map<String, Object> guildConfigMap = (Map<String, Object>) guildYaml.load(guildConfigString);


        // Get the allowed text channels.
        String textChannelString = (String) guildConfigMap.get("textChannels");
        HashSet<Long> textChannelsSet = new HashSet<>();
        if (!textChannelString.equals("")) {
            String[] textChannels = textChannelString.replaceAll("[\\[\\]]", "").split(", ");
            for (String textChannel : textChannels) {
                long textChannelId = Long.parseLong(textChannel);
                textChannelsSet.add(textChannelId);
            }
        }
        guildAllowedTextChannels.put(guildId, textChannelsSet);

        // Get the allowed voice channels.
        String voiceChannelString = (String) guildConfigMap.get("voiceChannels");
        HashSet<Long> voiceChannelsSet = new HashSet<>();
        if (!voiceChannelString.equals("")) {
            String[] voiceChannels = voiceChannelString.replaceAll("[\\[\\]]", "").split(", ");
            for (String voiceChannel : voiceChannels) {
                long voiceChannelId = Long.parseLong(voiceChannel);
                voiceChannelsSet.add(voiceChannelId);
            }
        }
        guildAllowedVoiceChannels.put(guildId, voiceChannelsSet);

        // Get the admins.
        String adminString = (String) guildConfigMap.get("admins");
        HashSet<Long> adminsSet = new HashSet<>();

        if (!adminString.equals("")) {
            String[] admins = adminString.replaceAll("[\\[\\]]", "")
                    .split(", ");
            for (String admin : admins) {
                long adminId = Long.parseLong(admin);
                adminsSet.add(adminId);
            }
        }
        guildAdmins.put(guildId, adminsSet);

        // Get the music roles.
        String musicRolesString = (String) guildConfigMap.get("musicRoles");
        HashSet<Long> musicRolesSet = new HashSet<>();
        if (!musicRolesString.equals("")) {
            String[] musicRoles = musicRolesString.replaceAll("[\\[\\]]", "").split(", ");
            for (String musicRole : musicRoles) {
                long musicRoleId = Long.parseLong(musicRole);
                musicRolesSet.add(musicRoleId);
            }
        }
        guildMusicRoles.put(guildId, musicRolesSet);
    }

    public Config(String credsFilePath) {
        try {
            Yaml yaml = new Yaml();
            File credsFile = new File(credsFilePath);
            String credsFileString = FileUtils.readFileToString(credsFile);
            Map<String, Object> creds = (Map<String, Object>) yaml.load(credsFileString);
            botToken = (String) creds.get("token");

            filePath = (String) creds.get("filePath");

            osName = System.getProperty("os.name", "generic")
                    .toLowerCase(Locale.ENGLISH);
            if (osName.indexOf("win") >= 0) {
                guildDataFilepath = filePath + "\\discord-dau-config\\";
            } else if (osName.indexOf("nux") >= 0){
                guildDataFilepath = filePath + "/discord-dau-config/";
            } else {
                guildDataFilepath = null;
            }

            owner = (long) creds.get("adminID");
            youtubeAPIKey = (String) creds.get("youtubeAPIKey");
            String testing = (String) creds.get("testing");

            if (testing.equals("yes")) {
                isTesting = true;
            } else {
                isTesting = false;
            }
            guildFilePaths = new HashMap<>();

            guildAllowedTextChannels = new HashMap<>();
            guildAllowedVoiceChannels = new HashMap<>();
            guildAdmins = new HashMap<>();
            guildMusicRoles = new HashMap<>();

            // For each guild, each guild should have a link
            // to the path for configuration settings.
            File folder = new File(guildDataFilepath);
            File[] files = folder.listFiles();
            System.out.println("Got here");
            for (File guildFile : files) {
                System.out.println("Got here2");
                initializeGuildData(guildFile, creds);
            }

            startTime = System.currentTimeMillis();
        } catch (IOException e) {
            System.out.println("File cannot be opened. Exception: " + e + ".");
            throw new NullPointerException();
        }
        // For guilds with specific settings, have a field for selected guilds.
    }

    public boolean isTesting() {
        return isTesting;
    }

    public static boolean setUpConfig(String[] parameters) {
        if (parameters == null) {
            System.out.println("File path of creds file not supplied.");
            return false;
        }
        Config.CONFIG = new Config(parameters[0]);
        return true;
    }

    public static String getBotToken() {
        return botToken;
    }

    public static String getFilePath() {
        return filePath;
    }

    public static long getAdminID() {
        return owner;
    }

    public static String getOsName() {
        return osName;
    }

    public static String getYoutubeAPIKey() {
        return youtubeAPIKey;
    }

}
