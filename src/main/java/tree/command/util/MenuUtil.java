package tree.command.util;

import net.dv8tion.jda.core.entities.*;
import tree.command.music.AddCommand;
import tree.commandutil.type.Command;
import tree.commandutil.util.CommandRegistry;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Admin on 8/15/2017.
 */
public class MenuUtil {
    private static final String[] MENU_COMMANDS = {"add", "req", "info"};
    private static MenuUtil menuUtil = new MenuUtil();
    private static Map<String, ArrayList<Map>> commandMenuMap;
    private ScheduledExecutorService scheduler;
    public static final int MENU = 0;
    public static final int USER = 1;

    private MenuUtil() {
        commandMenuMap = new HashMap<>();
        scheduler = Executors.newScheduledThreadPool(3);
        for (String commandName : MENU_COMMANDS) {
            ArrayList<Map> menuList = new ArrayList<>();
            menuList.add(MENU, new HashMap<Long, Long>());
            menuList.add(USER, new HashMap<Long, Long>());
            commandMenuMap.put(commandName, menuList);
        }
    }

    public boolean inSameMessageChannel(MessageChannel msgChan, String commandName) {
        Map<Long, Long> channelMenuMap = commandMenuMap.get(commandName).get(MENU);
        return channelMenuMap.containsKey(msgChan.getIdLong());
    }

    public ScheduledFuture<?> createMenuTask(Runnable runnable, ScheduledFuture<?> task,
                                             int timeDelaySeconds) {
        if (task != null && scheduler != null) {
            task.cancel(true);
        }

        if (task == null || task.isCancelled() || task.isDone()) {
            return scheduler.schedule(
                    runnable, timeDelaySeconds, TimeUnit.SECONDS);
        } else {
            return null;
        }

    }

    public void deleteMenu(MessageChannel msgChan, String commandName) {
        Map<Long, Long> channelMenuMap = commandMenuMap.get(commandName).get(MENU);
        Map<Long, Long> channelUserMap = commandMenuMap.get(commandName).get(USER);
        channelUserMap.remove(msgChan.getIdLong());
        // If no choice has been selected, pick the first song to add.
        if (!channelMenuMap.containsKey(msgChan.getIdLong())) {
            return;
        }
        long messageId = channelMenuMap.remove(msgChan.getIdLong());
        msgChan.deleteMessageById(messageId).queue();
    }

    public static MenuUtil getInstance() {
        return menuUtil;
    }

    public void setUserId(String commandName, MessageChannel msgChanId, Member member) {
        Map<Long, Long> channelUserMap = commandMenuMap.get(commandName).get(USER);
        channelUserMap.put(msgChanId.getIdLong(), member.getUser().getIdLong());
    }

    public long getUserId(String commandName, MessageChannel msgChanId) {
        Map<Long, Long> channelUserMap = commandMenuMap.get(commandName).get(USER);
        if (!channelUserMap.containsKey(msgChanId.getIdLong())) {
            return -1;
        }
        return channelUserMap.get(msgChanId.getIdLong());
    }

    public long removeUserId(String commandName, MessageChannel msgChanId) {
        Map<Long, Long> channelUserMap = commandMenuMap.get(commandName).get(USER);
        if (!channelUserMap.containsKey(msgChanId.getIdLong())) {
            return -1;
        }
        return channelUserMap.remove(msgChanId.getIdLong());
    }

    public void setMenuId(String commandName, MessageChannel msgChanId, Message message) {
        Map<Long, Long> channelMenuMap = commandMenuMap.get(commandName).get(MENU);
        channelMenuMap.put(msgChanId.getIdLong(), message.getIdLong());
    }

    public long getMenuId(String commandName, MessageChannel msgChanId) {
        Map<Long, Long> channelMenuMap = commandMenuMap.get(commandName).get(MENU);
        if (!channelMenuMap.containsKey(msgChanId.getIdLong())) {
            return -1;
        }
        return channelMenuMap.get(msgChanId.getIdLong());
    }

    public long removeMenuId(String commandName, MessageChannel msgChanId) {
        Map<Long, Long> channelMenuMap = commandMenuMap.get(commandName).get(MENU);
        if (!channelMenuMap.containsKey(msgChanId.getIdLong())) {
            return -1;
        }
        return channelMenuMap.remove(msgChanId.getIdLong());
    }
}
