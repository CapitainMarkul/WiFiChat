package ru.palestra.wifichat.domain.db.command;

import ru.palestra.wifichat.data.models.daomodels.DaoSession;
import ru.palestra.wifichat.data.models.daomodels.MessageSql;
import ru.palestra.wifichat.data.models.daomodels.MessageSqlDao;
import ru.palestra.wifichat.data.models.viewmodels.Message;

/**
 * Created by da.pavlov1 on 16.11.2017.
 */

public class UpdateMsgStatusCommand implements DbCommand<MessageSql> {
    private final Message deliveredMessage;

    public UpdateMsgStatusCommand(Message deliveredMessage) {
        this.deliveredMessage = deliveredMessage;
    }

    @Override
    public MessageSql execute(DaoSession daoSession) {
        synchronized (DaoSession.class) {
            final MessageSqlDao messageSqlDao = daoSession.getMessageSqlDao();

            MessageSql updatedMessageSql =
                    messageSqlDao.queryBuilder()
                            .where(MessageSqlDao.Properties.MessageUUID.eq(deliveredMessage.getMsgUUID()))
                            .unique();

            updatedMessageSql.setStatusDelivered(true);
            messageSqlDao.update(updatedMessageSql);

            return updatedMessageSql;
        }
    }
}
