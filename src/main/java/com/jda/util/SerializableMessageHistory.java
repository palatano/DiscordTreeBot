package com.jda.util;

import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageHistory;

import java.io.Serializable;

/**
 * Created by Valued Customer on 7/24/2017.
 */
public class SerializableMessageHistory extends MessageHistory implements Serializable {

    public SerializableMessageHistory(MessageChannel msgChan) {
        super(msgChan);
    }

}
