package ru.palestra.wifichat.data.models.mappers;

import java.util.ArrayList;
import java.util.List;

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

    public static List<Client> toListClientView(List<ClientSql> clientSqls) {
        List<Client> clients = new ArrayList<>();

        for (ClientSql clientSql : clientSqls) {
            clients.add(toClientView(clientSql));
        }
        return clients;
    }

    public static ClientSql toClientDb(Client client) {
        return new ClientSql(null, client.getName(), client.getUUID());
    }
}
