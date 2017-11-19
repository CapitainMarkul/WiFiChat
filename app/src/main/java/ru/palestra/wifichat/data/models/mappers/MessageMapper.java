package ru.palestra.wifichat.data.models.mappers;

import java.util.ArrayList;
import java.util.List;

import ru.palestra.wifichat.data.models.daomodels.MessageSql;
import ru.palestra.wifichat.data.models.viewmodels.Message;

/**
 * Created by da.pavlov1 on 16.11.2017.
 */

public class MessageMapper {
    private MessageMapper() {

    }

    public static Message toMessageView(MessageSql messageSql) {
        return Message.newMessage(messageSql.getFromName(), messageSql.getFromUUID(), null, null, messageSql.getText());
    }

    public static List<Message> toListMessageView(List<MessageSql> messageSqls) {
        List<Message> messages = new ArrayList<>();

        for (MessageSql messageSql : messageSqls) {
            messages.add(toMessageView(messageSql));
        }
        return messages;
    }

    public static MessageSql toMessageDb(Message message) {
        return new MessageSql(null, message.getFromName(), message.getFromUUID(), message.getTargetUUID(), message.getMsgUUID(), message.getText(), false, message.getTimeSend());
    }
}
