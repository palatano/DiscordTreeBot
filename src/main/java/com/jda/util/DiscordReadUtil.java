package com.jda.util;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Valued Customer on 7/20/2017.
 */
public class DiscordReadUtil {
    Map<String, String> userMap;

    public DiscordReadUtil() {
        userMap = new HashMap<>();
    }

    public void getPublicData(MessageReceivedEvent event, String publicMessage) {
        String userName = event.getMember().getEffectiveName();
        if (!userMap.containsKey(userName)) {
            userMap.put(userName, publicMessage);
        }
    }

    // Need database along with identifier for each unique
}
