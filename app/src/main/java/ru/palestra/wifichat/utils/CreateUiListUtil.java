package ru.palestra.wifichat.utils;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.palestra.wifichat.data.models.mappers.ClientMapper;
import ru.palestra.wifichat.data.models.viewmodels.Client;
import ru.palestra.wifichat.domain.db.DbClient;

/**
 * Created by Dmitry on 19.11.2017.
 */

public class CreateUiListUtil {
    private static List<Client> uiClients = new ArrayList<>(); //Лист, для отображения
    private final DbClient dbClient;

    private CreateUiListUtil(@NonNull DbClient dbClient) {
        this.dbClient = dbClient;

        //Заружаем оффлайн список клиентов
        uiClients =
                ClientMapper.toListClientView(dbClient.getAllWasConnectedClients());
    }

    public static void init(@NonNull DbClient dbClient) {
        new CreateUiListUtil(dbClient);
    }

    public static void clientConnected(String idEndPoint, Client client) {
        updateUiList(Collections.singletonList(Client.isOnline(idEndPoint, client)));
    }

    public static void clientDisconnect(Client client) {
        updateUiList(Collections.singletonList(Client.isOffline(client)));
    }

    public static ArrayList<Client> getUiClients() {
        return (ArrayList<Client>) uiClients;
    }

    private static List<Client> updateUiList(List<Client> connectedClients) {
        Client[] oldClientArray = new Client[uiClients.size()];
        oldClientArray = uiClients.toArray(oldClientArray);

        for (Client newClient : connectedClients) {
            boolean isNewClient = true;
            for (Client oldClient : oldClientArray) {
                if (oldClient.getUUID().equals(newClient.getUUID()) &&
                        oldClient.isOnline() != newClient.isOnline()) {
                    uiClients.remove(oldClient);
                    uiClients.add(newClient);
                    isNewClient = false;
                    break;
                }
            }

            if (isNewClient) {
                uiClients.add(newClient);
            }
        }

        return uiClients;
    }
}
