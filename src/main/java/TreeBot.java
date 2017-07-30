import commandutil.CommandManager;
import event.TreeListener;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import okhttp3.OkHttpClient;

import java.util.concurrent.TimeUnit;

/**
 * Created by Admin on 7/29/2017.
 */
public class TreeBot extends TreeMain {
    private TreeListener listener;
    private static JDA jda;

    private static OkHttpClient.Builder setupBuilder(OkHttpClient.Builder builder) {
        builder = builder.connectTimeout(60000, TimeUnit.MILLISECONDS);
        builder = builder.readTimeout(60000, TimeUnit.MILLISECONDS);
        builder = builder.writeTimeout(60000, TimeUnit.MILLISECONDS);
        return builder;
    }

    public TreeBot(int id, TreeListener listener) {
        this.listener = listener;
        jda.addEventListener(listener);
        CommandManager.init();
    }

    public static JDA setUp() {
        try {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            jda = new JDABuilder(AccountType.BOT)
                .setHttpClientBuilder(setupBuilder(builder))
                .setToken(Config.CONFIG.getBotToken())
                .buildBlocking();
            return jda;
        } catch (Exception e) {
            System.out.println("Failed to setup the bot with exception " + e + ". Try again later.");
            return null;
        }
    }
}
