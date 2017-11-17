package ru.palestra.wifichat.data.models.viewmodels;

/**
 * Created by da.pavlov1 on 17.11.2017.
 */

public class ClientMessageWrap {
    private final Client client;
    private final Message lastMessage;

    public ClientMessageWrap(Client client, Message lastMessage) {
        this.client = client;
        this.lastMessage = lastMessage;
    }

    public Client getClient() {
        return client;
    }

    public Message getLastMessage() {
        return lastMessage;
    }
}
