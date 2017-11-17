package ru.palestra.wifichat.domain.db.command;

import java.util.List;

import ru.palestra.wifichat.data.models.daomodels.DaoSession;
import ru.palestra.wifichat.data.models.daomodels.MessageSql;
import ru.palestra.wifichat.data.models.daomodels.MessageSqlDao;

/**
 * Created by da.pavlov1 on 16.11.2017.
 */

public class GetAllMsgFromClient implements DbCommand<List<MessageSql>> {
    private final String senderUUID;

    public GetAllMsgFromClient(String senderUUID) {
        this.senderUUID = senderUUID;
    }

    @Override
    public List<MessageSql> execute(DaoSession daoSession) {
        synchronized (DaoSession.class) {
            final MessageSqlDao messageSqlDao = daoSession.getMessageSqlDao();

            return messageSqlDao.queryBuilder()
                    .where(MessageSqlDao.Properties.FromUUID.eq(senderUUID))
                    .list();
        }
    }
}
