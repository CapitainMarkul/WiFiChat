package ru.palestra.wifichat.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.palestra.wifichat.R;
import ru.palestra.wifichat.model.Message;

/**
 * Created by Dmitry on 01.11.2017.
 */

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.ViewHolder> {
    private List<Message> messages = new ArrayList<>();
    private String myDeviceName;

    public void setCurrentDevice(String myDeviceName) {
        this.myDeviceName = myDeviceName;
    }

    public void setMessages(Message message) {
        if (!messages.contains(message)) {
            messages.add(message);
            sortMessages();

            notifyDataSetChanged();
        }
    }

    private void sortMessages() {
        Collections.sort(messages, (message1, message2) -> {
            int hourMessage1 = message1.getTimeSend().getHour();
            int hourMessage2 = message2.getTimeSend().getHour();
            int minuteMessage1 = message1.getTimeSend().getMinute();
            int minuteMessage2 = message2.getTimeSend().getMinute();

            if (hourMessage1 > hourMessage2) {
                return 1;
            } else if (hourMessage1 < hourMessage2) {
                return -1;
            } else {
                if (minuteMessage1 > minuteMessage2) {
                    return 1;
                } else if (minuteMessage1 < minuteMessage2) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });
    }

    @Override
    public MessagesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        MessagesAdapter.ViewHolder vh = new MessagesAdapter.ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(MessagesAdapter.ViewHolder holder, int position) {
        Message currentMessage = messages.get(position);
        if (myDeviceName != null) {
            if (currentMessage.getFrom().equals(myDeviceName)) {
                holder.message.setText(
                        String.format("ME: %s", currentMessage.getText()));
            } else {
                holder.message.setText(
                        String.format("%s: %s", currentMessage.getFrom(), currentMessage.getText()));
            }
            holder.timeSend.setText(
                    String.format("%s:%s", currentMessage.getTimeSend().getHour(), currentMessage.getTimeSend().getMinute()));
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView message;
        private TextView timeSend;

        public ViewHolder(View itemView) {
            super(itemView);

            message = itemView.findViewById(R.id.txt_item_message);
            timeSend = itemView.findViewById(R.id.txt_time_send);
        }
    }
}
