package ru.palestra.wifichat.domain.db.command;

import ru.palestra.wifichat.data.models.daomodels.ClientSql;
import ru.palestra.wifichat.data.models.daomodels.ClientSqlDao;
import ru.palestra.wifichat.data.models.daomodels.DaoSession;

/**
 * Created by da.pavlov1 on 16.11.2017.
 */

public class SaveConnectedClientCommand implements DbCommand<ClientSql> {
    private final ClientSql clientForSave;

    public SaveConnectedClientCommand(ClientSql clientForSave) {
        this.clientForSave = clientForSave;
    }

    @Override
    public ClientSql execute(DaoSession daoSession) {
        synchronized (DaoSession.class) {
            final ClientSqlDao clientSqlDao = daoSession.getClientSqlDao();

            clientSqlDao.insert(clientForSave);
            return clientForSave;
        }
    }
}
