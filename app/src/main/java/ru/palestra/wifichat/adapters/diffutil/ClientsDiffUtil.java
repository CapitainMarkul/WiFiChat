package ru.palestra.wifichat.adapters.diffutil;

import android.support.v7.util.DiffUtil;

import java.util.List;

import ru.palestra.wifichat.data.models.viewmodels.Client;

/**
 * Created by Dmitry on 18.11.2017.
 */

public class ClientsDiffUtil extends DiffUtil.Callback {
    private final List<Client> oldList;
    private final List<Client> newList;

    public ClientsDiffUtil(List<Client> oldList, List<Client> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return oldList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        Client oldClient = oldList.get(oldItemPosition);
        Client newClient = newList.get(newItemPosition);
        return oldClient.getUUID().equals(newClient.getUUID());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        Client oldClient = oldList.get(oldItemPosition);
        Client newClient = newList.get(newItemPosition);
        return oldClient.isOnline() != newClient.isOnline();
    }
}
