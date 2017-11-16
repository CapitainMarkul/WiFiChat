package ru.palestra.wifichat.domain.db.command;

import java.util.List;

import ru.palestra.wifichat.data.models.daomodels.ClientSql;
import ru.palestra.wifichat.data.models.daomodels.DaoSession;
import ru.palestra.wifichat.data.models.daomodels.MessageSql;

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
        return null;
    }
}
