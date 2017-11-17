package ru.palestra.wifichat.domain.db.command;

import ru.palestra.wifichat.data.models.daomodels.ClientSql;
import ru.palestra.wifichat.data.models.daomodels.ClientSqlDao;
import ru.palestra.wifichat.data.models.daomodels.DaoSession;

/**
 * Created by da.pavlov1 on 16.11.2017.
 */

public class CheckConnectedClientCommand implements DbCommand<ClientSql> {
    private final String clientUUID;

    public CheckConnectedClientCommand(String clientUUID) {
        this.clientUUID = clientUUID;
    }

    @Override
    public ClientSql execute(DaoSession daoSession) {
        synchronized (DaoSession.class) {
            final ClientSqlDao clientSqlDao = daoSession.getClientSqlDao();

            return clientSqlDao.queryBuilder()
                    .where(ClientSqlDao.Properties.UUID.eq(clientUUID))
                    .unique();
        }
    }
}
