package tree.command.util;

import net.dv8tion.jda.core.entities.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Admin on 8/15/2017.
 */
public class MenuUtil {
    private static MenuUtil menuUtil = new MenuUtil();
    private static Map<Long, Long> channelMenuMessageMap;
    private static Map<Long, Long> channelMenuUserMap;

    private MenuUtil() {
        channelMenuMessageMap = new HashMap<>();
        channelMenuUserMap = new HashMap<>();
    }

    public void cancelMenu(Guild guild, MessageChannel msgChan, Message message, Member member,
                           ScheduledFuture<?> menuSelectionTask, AtomicBoolean waitingForChoice) {
        channelMenuUserMap.remove(msgChan.getIdLong());
        waitingForChoice.set(false);
        // If no choice has been selected, pick the first song to add.
        if (!menuSelectionTask.isCancelled()) {
            menuSelectionTask.cancel(true);
        }
        if (!channelMenuMessageMap.containsKey(msgChan.getIdLong())) {
            return;
        }
        long messageId = channelMenuMessageMap.remove(msgChan.getIdLong());
        msgChan.deleteMessageById(messageId).queue();
    }

    public static MenuUtil getInstance() {
        return menuUtil;
    }

    public void setUserId(long msgChanId, long userId) {
        channelMenuUserMap.put(msgChanId, userId);
    }

    public long getUserId(long msgChanId) {
        if (!channelMenuUserMap.containsKey(msgChanId)) {
            return -1;
        }
        return channelMenuUserMap.get(msgChanId);
    }

    public long removeUserId(long msgChanId) {
        if (!channelMenuUserMap.containsKey(msgChanId)) {
            return -1;
        }
        return channelMenuUserMap.remove(msgChanId);
    }

    public void setMenuId(long msgChanId, long menuId) {
        channelMenuMessageMap.put(msgChanId, menuId);
    }

    public long getMenuId(long msgChanId) {
        if (!channelMenuMessageMap.containsKey(msgChanId)) {
            return -1;
        }
        return channelMenuMessageMap.get(msgChanId);
    }

    public long removeMenuId(long msgChanId) {
        if (!channelMenuMessageMap.containsKey(msgChanId)) {
            return -1;
        }
        return channelMenuMessageMap.remove(msgChanId);
    }
}
