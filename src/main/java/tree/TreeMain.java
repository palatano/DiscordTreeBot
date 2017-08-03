package tree;

import tree.commandutil.CommandManager;
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

    public static void main(String[] args)
            throws LoginException, RateLimitedException, InterruptedException {
        /* Get the credentials file. */
        Config.setUpConfig(args);
        listener = new TreeListener();
        TreeBot.setUp();
        addListener(listener);
        /* Create the bot and add the listeners for the bot. */
    }

    /**
     * Confirm that the user is palat, so the commands won't be abused.
     * @param event - event of a received message.
     * @return boolean depending if user is palat.
     */

    private static void addListener(TreeListener listener) {
        shards.add(new TreeBot(0, listener));
    }

}
