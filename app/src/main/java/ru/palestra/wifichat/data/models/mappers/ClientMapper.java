package ru.palestra.wifichat.data.models.mappers;

import ru.palestra.wifichat.data.models.daomodels.ClientSql;
import ru.palestra.wifichat.data.models.viewmodels.Client;

/**
 * Created by da.pavlov1 on 16.11.2017.
 */

public class ClientMapper {
    private ClientMapper() {

    }

    public static Client toClientView(ClientSql clientSql) {
        return Client.otherDevice(clientSql.getName(), null, clientSql.getUUID());
    }

    public static ClientSql toClientDb(Client client) {
        return new ClientSql(null, client.getClientName(), client.getUUID());
    }
}
