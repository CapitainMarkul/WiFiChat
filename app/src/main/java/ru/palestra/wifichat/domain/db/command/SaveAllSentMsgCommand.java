package ru.palestra.wifichat.domain.db.command;

import java.util.List;

import ru.palestra.wifichat.data.models.daomodels.DaoSession;
import ru.palestra.wifichat.data.models.daomodels.MessageSql;
import ru.palestra.wifichat.data.models.daomodels.MessageSqlDao;

/**
 * Created by da.pavlov1 on 16.11.2017.
 */

public class SaveAllSentMsgCommand implements DbCommand<List<MessageSql>> {
    private final List<MessageSql> sentMessagesSql;

    public SaveAllSentMsgCommand(List<MessageSql> messageSql) {
        this.sentMessagesSql = messageSql;
    }

    @Override
    public List<MessageSql> execute(DaoSession daoSession) {
        synchronized (DaoSession.class) {
            final MessageSqlDao messageSqlDao = daoSession.getMessageSqlDao();

            messageSqlDao.deleteAll();
            messageSqlDao.insertInTx(sentMessagesSql);
            return sentMessagesSql;
        }
    }
}
