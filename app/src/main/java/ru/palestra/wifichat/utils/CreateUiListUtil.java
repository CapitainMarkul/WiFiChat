package ru.palestra.wifichat.utils;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.palestra.wifichat.data.models.mappers.ClientMapper;
import ru.palestra.wifichat.data.models.viewmodels.Client;
import ru.palestra.wifichat.data.models.viewmodels.Message;
import ru.palestra.wifichat.domain.db.DbClient;

/**
 * Created by Dmitry on 19.11.2017.
 */

public class CreateUiListUtil {
    private static List<Client> uiClients = new ArrayList<>(); //Лист, для отображения

    private CreateUiListUtil(@NonNull DbClient dbClient) {

        //Заружаем оффлайн список клиентов
        uiClients =
                ClientMapper.toListClientView(dbClient.getAllWasConnectedClients());
    }

    public static void init(@NonNull DbClient dbClient) {
        new CreateUiListUtil(dbClient);
    }

    public static void clientConnected(String idEndPoint, Client client) {
        client.setNearbyKey(idEndPoint);
        client.setOnline(true);

        updateUiClientsList(Collections.singletonList(client));
    }

    public static void clientDisconnect(Client client) {
        client.setOnline(false);

        updateUiClientsList(Collections.singletonList(client));
    }

    private static void updateUiClientsList(List<Client> connectedClients) {
        Client[] oldClientArray = new Client[uiClients.size()];
        oldClientArray = uiClients.toArray(oldClientArray);

        for (Client newClient : connectedClients) {
            boolean isNewClient = true;
            for (Client oldClient : oldClientArray) {
                if (oldClient.getUUID().equals(newClient.getUUID())) {
                    int indexOldClient = uiClients.indexOf(oldClient);

                    uiClients.remove(indexOldClient);
                    uiClients.add(indexOldClient, newClient);
                    isNewClient = false;
                    break;
                }
            }

            if (isNewClient) {
                uiClients.add(newClient);
            }
        }
    }

    public static ArrayList<Client> getUiClients() {
        return (ArrayList<Client>) uiClients;
    }

    public static List<Message> createUiMessagesList(List<Message> uiListMessages, Message newMessage) {
        Message[] oldMessagesArray = new Message[uiListMessages.size()];
        oldMessagesArray = uiListMessages.toArray(oldMessagesArray);

        if (!uiListMessages.contains(newMessage)) {
            for (Message oldMessage : oldMessagesArray) {
                if (oldMessage.getMsgUUID().equals(newMessage.getMsgUUID())) {
                    //Обновляем сообщение
                    int removedIndex = uiListMessages.indexOf(oldMessage);

                    uiListMessages.remove(removedIndex);
                    uiListMessages.add(removedIndex, newMessage);

                    return uiListMessages;
                }
            }
            //Если это новое сообщение
            uiListMessages.add(newMessage);
        }
        return uiListMessages;
    }
}
