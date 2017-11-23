package ru.palestra.wifichat.utils;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import ru.palestra.wifichat.data.models.daomodels.ClientSql;
import ru.palestra.wifichat.data.models.mappers.ClientMapper;
import ru.palestra.wifichat.data.models.viewmodels.Client;
import ru.palestra.wifichat.data.models.viewmodels.Message;

/**
 * Created by Dmitry on 19.11.2017.
 */

public class CreateUiListUtil {
    private static List<Client> uiClients = new ArrayList<>(); //Лист, для отображения

    private CreateUiListUtil() {

    }

    //Заружаем оффлайн список клиентов
    public static void init(@NonNull List<ClientSql> clientSqls) {
        uiClients.clear();
        uiClients.addAll(ClientMapper.toListClientView(clientSqls));
    }

    public static ArrayList<Client> updateUiClientsList(Client client) {
        Client[] oldClientArray = new Client[uiClients.size()];
        oldClientArray = uiClients.toArray(oldClientArray);

        boolean isNewClient = true;
        for (Client oldClient : oldClientArray) {
            if (oldClient.getUUID().equals(client.getUUID())) {
                int indexOldClient = uiClients.indexOf(oldClient);

                uiClients.remove(indexOldClient);
                uiClients.add(indexOldClient, client);
                isNewClient = false;
                break;
            }
        }

        if (isNewClient) {
            uiClients.add(client);
        }

        return (ArrayList<Client>) uiClients;
    }

    public static ArrayList<Client> getUiClients() {
        return (ArrayList<Client>) uiClients;
    }

    public static void clearViewClients() {
        uiClients.clear();
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
