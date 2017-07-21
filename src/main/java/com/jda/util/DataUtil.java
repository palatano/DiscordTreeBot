package com.jda.util;

import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Valued Customer on 7/21/2017.
 */
public class DataUtil {
    private Map<String, Object> creds;

    public Map<String, Object> getCreds() {
        return creds;
    }

    public void setCreds(Map<String, Object> creds) {
        this.creds = creds;
    }

    /**
     * Get the credentials data structure for logging the bot into the server.
     * @param file - The filename argument.
     * @return The credentials data structure.
     */
    public static Map<String, Object> retrieveCreds(String file) {
        Map<String, Object> creds = null;
        try {
            File credsFile = new File(file);
            Yaml yaml = new Yaml();
            String credsFileString = FileUtils.readFileToString(credsFile);
            creds = (Map<String, Object>) yaml.load(credsFileString);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
        return creds;
    }
}
