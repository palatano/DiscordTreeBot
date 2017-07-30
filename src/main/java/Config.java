import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import okhttp3.OkHttpClient;
import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by Admin on 7/29/2017.
 */
public class Config {
    public static Config CONFIG;

    private static String botToken;
    private static String filePath;
    private Logger logger;

    public Config(String credsFilePath) {
        try {
            Yaml yaml = new Yaml();
            File credsFile = new File(credsFilePath);
            String credsFileString = FileUtils.readFileToString(credsFile);
            Map<String, Object> creds = (Map<String, Object>) yaml.load(credsFileString);
            botToken = (String) creds.get("token");
            filePath = (String) creds.get("filePath");
        } catch (IOException e) {
            System.out.println("File cannot be opened. Exception: " + e + ".");
            throw new NullPointerException();
        }
//        DataUtil dataUtil = new DataUtil();
            // Set variables from the yaml database;

//        dataUtil.setCreds(creds);
//        /* Create the bot and add the listeners for the bot. */
//        String token = (String) creds.get("token");
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

}
