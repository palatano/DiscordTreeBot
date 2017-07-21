package com.jda.util;

import net.dv8tion.jda.core.entities.Message;

import java.time.OffsetDateTime;

/**
 * Created by Admin on 7/20/2017.
 */
public class MessageUtil {

    /**
     * Get the time (EST).
     * @return formatted time string.
     */
    private static String getTime(Message message) {
        OffsetDateTime time = message.getCreationTime();
        int hour = time.getHour() > 12 ? time.getHour() % 12 : time.getHour();
        String minute = time.getMinute() < 10 ? "0" + time.getMinute() :
                Integer.toString(time.getMinute());
        String second = time.getSecond() < 10 ? "0" + time.getSecond() :
                Integer.toString(time.getSecond());
        String zone = time.getHour() > 12 ? "PM" : "AM";
        return hour + ":" + minute + ":" + second
                + " " + zone;
    }


    /**
     * Get the date.
     * @return formatted date string.
     */
    private static String getDate(Message msg) {
        OffsetDateTime date = msg.getCreationTime();
        return date.getMonth() + " " + date.getDayOfMonth() + ", " + date.getYear();
    }

    /**
     * Get the time stamp of a message from an event.
     * @return formatted time stamp string.
     */
    public static String timeStamp(Message msg) {
        return "[" + getDate(msg) + "][" + MessageUtil.getTime(msg) + "]";
    }

    /**
     * Get the user and message from an event.
     * @param msg - the current message.
     * @return formatted user and message string.
     */
    public static String userMsg(Message msg) {
        return String.format("[Chan: %s] %s: %s",
                msg.getTextChannel().getName(),
                msg.getMember().getEffectiveName(),
                msg.getContent());
    }
}
