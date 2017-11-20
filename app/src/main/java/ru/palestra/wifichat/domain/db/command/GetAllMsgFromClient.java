package ru.palestra.wifichat.domain.db.command;

import org.greenrobot.greendao.query.QueryBuilder;

import java.util.List;

import ru.palestra.wifichat.data.models.daomodels.DaoSession;
import ru.palestra.wifichat.data.models.daomodels.MessageSql;
import ru.palestra.wifichat.data.models.daomodels.MessageSqlDao;

/**
 * Created by da.pavlov1 on 16.11.2017.
 */

public class GetAllMsgFromClient implements DbCommand<List<MessageSql>> {
    private final String myUUID;
    private final String senderUUID;

    public GetAllMsgFromClient(String myUUID, String senderUUID) {
        this.myUUID = myUUID;
        this.senderUUID = senderUUID;
    }

    @Override
    public List<MessageSql> execute(DaoSession daoSession) {
        synchronized (DaoSession.class) {
            final MessageSqlDao messageSqlDao = daoSession.getMessageSqlDao();

            QueryBuilder<MessageSql> qb = messageSqlDao.queryBuilder();
            qb.where(
                    qb.or(qb.and(
                            MessageSqlDao.Properties.FromUUID.eq(senderUUID),
                            MessageSqlDao.Properties.TargetUUID.eq(myUUID)),
                            qb.and(
                                    MessageSqlDao.Properties.FromUUID.eq(myUUID),
                                    MessageSqlDao.Properties.TargetUUID.eq(senderUUID))));
            qb.orderAsc(MessageSqlDao.Properties.TimeSend);

            return qb.list();
        }
    }
}
