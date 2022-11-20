package ru.palestra.wifichat.domain.db.command;

import org.greenrobot.greendao.query.QueryBuilder;

import java.util.List;

import ru.palestra.wifichat.data.models.daomodels.DaoSession;
import ru.palestra.wifichat.data.models.daomodels.MessageSql;
import ru.palestra.wifichat.data.models.daomodels.MessageSqlDao;

/**
 * Created by da.pavlov1 on 16.11.2017.
 */

public class GetAllNotReadMsgForClient implements DbCommand<List<MessageSql>> {
    private final String myUUID;
    private final String senderUUID;

    public GetAllNotReadMsgForClient(String myUUID, String senderUUID) {
        this.myUUID = myUUID;
        this.senderUUID = senderUUID;
    }

    @Override
    public List<MessageSql> execute(DaoSession daoSession) {
        synchronized (DaoSession.class) {
            final MessageSqlDao messageSqlDao = daoSession.getMessageSqlDao();
            QueryBuilder<MessageSql> qbInner = messageSqlDao.queryBuilder();
            qbInner.orderAsc(MessageSqlDao.Properties.TimeSend);

            return qbInner.list();
        }
    }
}
