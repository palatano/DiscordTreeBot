package tree.command.data;

import com.google.api.services.youtube.model.SearchResult;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import org.jsoup.nodes.Element;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

/**
 * Created by Valued Customer on 8/23/2017.
 */
public class MenuSelectionInfo {
    private Message menu;
    private MessageChannel msgChan;
    private List<? extends Object> listOfChoices;
    private ScheduledFuture<?> task;

    public Message getMenu() {
        return menu;
    }

    public MessageChannel getChannel() {
        return msgChan;
    }

    public List<?> getListOfChoices() {
        if (listOfChoices == null) {
            return null;
        }
        if (!listOfChoices.isEmpty()) {
            if (listOfChoices.get(0) instanceof String) {
                return listOfChoices.stream()
                        .map(object -> Objects.toString(object, null))
                        .collect(Collectors.toList());
            } else if (listOfChoices.get(0) instanceof Long) {
                return listOfChoices.stream()
                        .map(object ->
                                Long.parseLong(String.valueOf(object)))
                        .collect(Collectors.toList());
            } else if (listOfChoices.get(0) instanceof SearchResult) {
                return listOfChoices.stream()
                        .map(SearchResult.class::cast)
                        .collect(Collectors.toList());
            } else if (listOfChoices.get(0) instanceof Element) {
                return listOfChoices.stream()
                        .map(Element.class::cast)
                        .collect(Collectors.toList());
            }
        }
        return null;
    }

    public MenuSelectionInfo(Message menu, MessageChannel msgChan,
                             List<?> listOfChoices, ScheduledFuture<?> task) {
        this.menu = menu;
        this.msgChan = msgChan;
        this.listOfChoices = listOfChoices;
        this.task = task;
    }

    public ScheduledFuture<?> getTask() {
        return task;
    }
}
