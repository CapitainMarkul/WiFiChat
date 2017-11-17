package ru.palestra.wifichat.domain.db.command;

import java.util.List;

import ru.palestra.wifichat.data.models.daomodels.ClientSql;
import ru.palestra.wifichat.data.models.daomodels.ClientSqlDao;
import ru.palestra.wifichat.data.models.daomodels.DaoSession;

/**
 * Created by da.pavlov1 on 16.11.2017.
 */

public class GetAllWasConnectedClients implements DbCommand<List<ClientSql>> {

    @Override
    public List<ClientSql> execute(DaoSession daoSession) {
        synchronized (DaoSession.class) {
            final ClientSqlDao clientSqlDao = daoSession.getClientSqlDao();

            return clientSqlDao.queryBuilder().list();
        }
    }
}
