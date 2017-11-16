package ru.palestra.wifichat.domain.db;

import java.util.List;

import ru.palestra.wifichat.data.models.daomodels.ClientSql;
import ru.palestra.wifichat.data.models.daomodels.DaoSession;
import ru.palestra.wifichat.data.models.daomodels.MessageSql;
import ru.palestra.wifichat.data.models.viewmodels.Client;
import ru.palestra.wifichat.data.models.viewmodels.Message;
import ru.palestra.wifichat.domain.db.command.DbCommand;
import ru.palestra.wifichat.domain.db.command.GetAllMsgFromClient;
import ru.palestra.wifichat.domain.db.command.GetAllWasConnectedClients;
import ru.palestra.wifichat.domain.db.command.SaveConnectedClientCommand;
import ru.palestra.wifichat.domain.db.command.SaveSendedMsgCommand;
import ru.palestra.wifichat.domain.db.command.UpdateMsgStatusCommand;

/**
 * Created by da.pavlov1 on 16.11.2017.
 */

public class DbClient {
    private final DaoSession daoSession;

    public DbClient(DaoSession daoSession) {
        this.daoSession = daoSession;
    }

    public List<ClientSql> getAllWasConnectedClients() {
        DbCommand<List<ClientSql>> command = new GetAllWasConnectedClients();
        return command.execute(daoSession);
    }

    public ClientSql saveConnectedClient(Client client) {
        DbCommand<ClientSql> command = new SaveConnectedClientCommand(client);
        return command.execute(daoSession);
    }

    // TODO: 16.11.2017 И т.д.
    public List<MessageSql> getAllMsgFromClient(String clientUUID) {
        DbCommand<List<MessageSql>> command = new GetAllMsgFromClient(clientUUID);
        return command.execute(daoSession);
    }

    public MessageSql saveSendedMsg(Message message) {
        DbCommand<MessageSql> command = new SaveSendedMsgCommand(message);
        return command.execute(daoSession);
    }

    public MessageSql updateMsgStatus(Message message) {
        DbCommand<MessageSql> command = new UpdateMsgStatusCommand(message);
        return command.execute(daoSession);
    }
}
