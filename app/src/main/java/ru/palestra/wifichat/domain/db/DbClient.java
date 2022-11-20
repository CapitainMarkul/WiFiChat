package ru.palestra.wifichat.domain.db;

import java.util.List;

import ru.palestra.wifichat.data.models.daomodels.ClientSql;
import ru.palestra.wifichat.data.models.daomodels.DaoSession;
import ru.palestra.wifichat.data.models.daomodels.MessageSql;
import ru.palestra.wifichat.data.models.viewmodels.Message;
import ru.palestra.wifichat.domain.db.command.CheckConnectedClientCommand;
import ru.palestra.wifichat.domain.db.command.DbCommand;
import ru.palestra.wifichat.domain.db.command.GetAllMsgFromClient;
import ru.palestra.wifichat.domain.db.command.GetAllNotReadMsgForClient;
import ru.palestra.wifichat.domain.db.command.GetAllWasConnectedClients;
import ru.palestra.wifichat.domain.db.command.SaveAllSentMsgCommand;
import ru.palestra.wifichat.domain.db.command.SaveConnectedClientCommand;
import ru.palestra.wifichat.domain.db.command.SaveSentMsgCommand;
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

    public ClientSql checkConnectedClient(String clientUUID) {
        DbCommand<ClientSql> command = new CheckConnectedClientCommand(clientUUID);
        return command.execute(daoSession);
    }

    public ClientSql saveConnectedClient(ClientSql clientSql) {
        DbCommand<ClientSql> command = new SaveConnectedClientCommand(clientSql);
        return command.execute(daoSession);
    }

    public List<MessageSql> getAllMsgFromClient(String myUUID, String clientUUID) {
        DbCommand<List<MessageSql>> command = new GetAllMsgFromClient(myUUID, clientUUID);
        return command.execute(daoSession);
    }

    public List<MessageSql> getAllNotReadMsgForClient(String myUUID, String clientUUID) {
        DbCommand<List<MessageSql>> command = new GetAllNotReadMsgForClient(myUUID, clientUUID);
        return command.execute(daoSession);
    }

    public MessageSql saveSentMsg(MessageSql messageSql) {
        DbCommand<MessageSql> command = new SaveSentMsgCommand(messageSql);
        return command.execute(daoSession);
    }

    public List<MessageSql> saveAllMsgs(List<MessageSql> messageSql) {
        DbCommand<List<MessageSql>> command = new SaveAllSentMsgCommand(messageSql);
        return command.execute(daoSession);
    }

    public MessageSql updateMsgStatus(Message message) {
        DbCommand<MessageSql> command = new UpdateMsgStatusCommand(message);
        return command.execute(daoSession);
    }
}
