package ru.palestra.wifichat.domain.db.command;

import ru.palestra.wifichat.data.models.daomodels.ClientSql;
import ru.palestra.wifichat.data.models.daomodels.DaoSession;
import ru.palestra.wifichat.data.models.viewmodels.Client;

/**
 * Created by da.pavlov1 on 16.11.2017.
 */

public class SaveConnectedClientCommand implements DbCommand<ClientSql> {
    private final Client clientForSave;

    public SaveConnectedClientCommand(Client clientForSave) {
        this.clientForSave = clientForSave;
    }

    @Override
    public ClientSql execute(DaoSession daoSession) {
        return null;
    }
}
