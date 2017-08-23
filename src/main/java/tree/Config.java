package tree;

import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import tree.db.JDBCInit;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by Admin on 7/29/2017.
 */
public class Config {
    public static Config CONFIG;
    private static String botToken;
    private static String filePath;
    private static String osName;
    private static long adminID;
    private static String youtubeAPIKey;
    private static boolean isTesting = false;
    private static Map<String, String> guildWithChannels;

    public Config(String credsFilePath) {
        try {
            Yaml yaml = new Yaml();
            File credsFile = new File(credsFilePath);
            String credsFileString = FileUtils.readFileToString(credsFile);
            Map<String, Object> creds = (Map<String, Object>) yaml.load(credsFileString);
            botToken = (String) creds.get("token");
            filePath = (String) creds.get("filePath");
            adminID = (long) creds.get("adminID");
            youtubeAPIKey = (String) creds.get("youtubeAPIKey");
            String testing = (String) creds.get("testing");
            if (testing.equals("yes")) {
                isTesting = true;
            } else {
                isTesting = false;
            }

            // Check if the guild database exists. If not, create.
            if (JDBCInit.hasTable("guilds")) {
                JDBCInit.createTable("guilds", "guildname VARCHAR(32)");
            }

            // Get the guilds to prepare.
            String guildsWithSettings = (String) creds.get("guildsWithSettings");
            String[] guilds = guildsWithSettings.split(", ");
            // Each consecutive guild should have a field in the yaml file, which is the channels
            // that allow bot commands to be in.
            for (String guild : guilds) {
                if (guild == null) {
                    System.out.println("There is no corresponding guild in the yaml.");
                    continue;
                }
                if (!JDBCInit.hasTable(guild)) {
                    JDBCInit.createTable(guild,
                            "permitted_channels BIGINT(64)",
                            "admin_roles VARCHAR(32)");
                }
                String guildChanString = (String) creds.get(guild);
                String[] guildChannels = guildChanString.split(", ");

                //
//                JDBCInit.insertGuildsInfo("guilds");

            }

            osName = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);

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
        return adminID;
    }

    public static String getOsName() {
        return osName;
    }

    public static String getYoutubeAPIKey() {
        return youtubeAPIKey;
    }

}
