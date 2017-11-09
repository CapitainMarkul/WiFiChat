package ru.palestra.wifichat.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ru.palestra.wifichat.R;

/**
 * Created by Dmitry on 01.11.2017.
 */

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.ViewHolder> {

    private List<String> messages = new ArrayList<>();

    public void setMessages(String textMessage) {
        if(!messages.contains(textMessage)) {
            messages.add(textMessage);
            notifyDataSetChanged();
        }
    }

    @Override
    public MessagesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        MessagesAdapter.ViewHolder vh = new MessagesAdapter.ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(MessagesAdapter.ViewHolder holder, int position) {
        holder.message.setText(messages.get(position));
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView message;

        public ViewHolder(View itemView) {
            super(itemView);

            message = itemView.findViewById(R.id.item_message);
        }
    }
}
