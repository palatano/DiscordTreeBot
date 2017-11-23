package tree.event;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.hooks.EventListener;
import net.dv8tion.jda.core.hooks.IEventManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Created by Valued Customer on 8/23/2017.
 */
public class AsynchEventManager implements IEventManager {
    private CopyOnWriteArrayList<EventListener> listeners =
            new CopyOnWriteArrayList<>();
    private ExecutorService threadPool;

    public AsynchEventManager() {
        ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat("Main Worker Thread Pool").build();
        threadPool = Executors.newFixedThreadPool(25, factory);
    }


    @Override
    public void register(Object o) {
        if (o instanceof EventListener) {
            listeners.add((EventListener) o);
        } else {
            throw new IllegalArgumentException(
                    "Argument must be an instance of EventListener.");
        }
    }

    @Override
    public void unregister(Object o) {
        if (o instanceof EventListener) {
            listeners.remove((EventListener) o);
        } else {
            throw new IllegalArgumentException(
                    "Argument must be an instance of EventListener.");
        }
    }

    @Override
    public void handle(Event event) {
        if (!threadPool.isShutdown() && !threadPool.isTerminated()) {
            // Create the runnable in the thread pool.
            threadPool.submit(() -> {
                listeners.forEach((listener) -> {
                    try {
                        listener.onEvent(event);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            });
        }
    }

    @Override
    public List<Object> getRegisteredListeners() {
        return new ArrayList<>(listeners);
    }
}