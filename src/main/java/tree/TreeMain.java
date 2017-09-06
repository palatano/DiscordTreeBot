package tree;

import net.dv8tion.jda.core.utils.SimpleLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tree.commandutil.CommandManager;
import tree.db.JDBCInit;
import tree.event.TreeListener;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import javax.security.auth.login.LoginException;
import java.util.*;

/**
 * Created by Valued Customer on 7/20/2017.
 */
public abstract class TreeMain extends ListenerAdapter {
    private CommandManager commandManager;
    private TreeBot treeBot;
    private static TreeListener listener;
    private static final List<TreeMain> shards = new ArrayList<>();
    private static Logger log = LoggerFactory.getLogger(TreeMain.class);

    public static void main(String[] args) {
        log.info(">>>>>>>> TreeMain Start <<<<<<<<");
//        SimpleLog.LEVEL = SimpleLog.Level.TRACE;

        // Get the credentials file.
//      JDBCInit db = new JDBCInit();
//      db.init();
        Config.setUpConfig(args);
        log.info("Configuration is complete.");

        // Create the bot and add the listeners for the bot.
        listener = new TreeListener();
        try {
            TreeBot.setUp();
            addListener(listener);
        } catch (LoginException | RateLimitedException | InterruptedException ex) {
            System.out.println(ex.getClass());
        }
    }

    private static void addListener(TreeListener listener) {
        shards.add(new TreeBot(0, listener));
    }

}
