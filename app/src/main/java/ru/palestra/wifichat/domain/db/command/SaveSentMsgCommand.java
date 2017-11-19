package ru.palestra.wifichat.domain.db.command;

import ru.palestra.wifichat.data.models.daomodels.DaoSession;
import ru.palestra.wifichat.data.models.daomodels.MessageSql;
import ru.palestra.wifichat.data.models.daomodels.MessageSqlDao;

/**
 * Created by da.pavlov1 on 16.11.2017.
 */

public class SaveSentMsgCommand implements DbCommand<MessageSql> {
    private final MessageSql sentMessageSql;

    public SaveSentMsgCommand(MessageSql sentMessageSql) {
        this.sentMessageSql = sentMessageSql;
    }

    @Override
    public MessageSql execute(DaoSession daoSession) {
        synchronized (DaoSession.class) {
            final MessageSqlDao messageSqlDao = daoSession.getMessageSqlDao();

            MessageSql messageSql = messageSqlDao.queryBuilder()
                    .where(MessageSqlDao.Properties.MessageUUID.eq(sentMessageSql.getMessageUUID()))
                    .unique();

            if(messageSql != null) return messageSql;

            messageSqlDao.insert(sentMessageSql);
            return sentMessageSql;
        }
    }
}
