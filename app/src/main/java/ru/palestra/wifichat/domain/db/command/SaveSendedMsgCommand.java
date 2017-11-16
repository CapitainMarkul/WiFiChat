package ru.palestra.wifichat.domain.db.command;

import ru.palestra.wifichat.data.models.daomodels.DaoSession;
import ru.palestra.wifichat.data.models.daomodels.MessageSql;
import ru.palestra.wifichat.data.models.viewmodels.Message;

/**
 * Created by da.pavlov1 on 16.11.2017.
 */

public class SaveSendedMsgCommand implements DbCommand<MessageSql> {
    private final Message sendedMessage;

    public SaveSendedMsgCommand(Message sendedMessage) {
        this.sendedMessage = sendedMessage;
    }

    @Override
    public MessageSql execute(DaoSession daoSession) {
        return null;
    }
}
