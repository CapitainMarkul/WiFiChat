package ru.palestra.wifichat.data.models.mappers;

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

    // TODO: 16.11.2017 Т.к. сообщения добавляем сразу, без факта доставки, то messageDelivered по умолчанию будет False
    public static MessageSql toMessageDb(Message message) {
        return new MessageSql(null, message.getFromName(), message.getFromUUID(), message.getUUID(), message.getText(), false, message.getTimeSend());
    }
}
