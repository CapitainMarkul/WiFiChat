package ru.palestra.wifichat.presentation.adapters.diffutil;

import androidx.recyclerview.widget.DiffUtil;

import java.util.List;

import ru.palestra.wifichat.data.models.viewmodels.Message;

/**
 * Created by Dmitry on 19.11.2017.
 */

public class MessageDiffUtil extends DiffUtil.Callback {
    private final List<Message> oldList;
    private final List<Message> newList;

    public MessageDiffUtil(List<Message> oldList, List<Message> newList) {
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
        Message oldMessage = oldList.get(oldItemPosition);
        Message newMessage = newList.get(newItemPosition);
        return oldMessage.getMsgUUID().equals(newMessage.getMsgUUID());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        Message oldMessage = oldList.get(oldItemPosition);
        Message newMessage = newList.get(newItemPosition);
        return oldMessage.isDelivered() == newMessage.isDelivered();
    }
}
