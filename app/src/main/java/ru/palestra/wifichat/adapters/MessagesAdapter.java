package ru.palestra.wifichat.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.threeten.bp.LocalDateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.palestra.wifichat.R;
import ru.palestra.wifichat.data.models.viewmodels.Message;
import ru.palestra.wifichat.utils.TimeUtils;

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
            LocalDateTime dateMessage1 = TimeUtils.longToLocalDateTime(message1.getTimeSend());
            LocalDateTime dateMessage2 = TimeUtils.longToLocalDateTime(message2.getTimeSend());

            if (dateMessage1.isAfter(dateMessage2)) {
                return 1;
            } else if (dateMessage1.isEqual(dateMessage2)) {
                return 0;
            } else {
                return -1;
            }
        });
    }

    @Override
    public MessagesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new MessagesAdapter.ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(MessagesAdapter.ViewHolder holder, int position) {
        Message currentMessage = messages.get(position);
        LocalDateTime dateCurrentMessage = TimeUtils.longToLocalDateTime(currentMessage.getTimeSend());

        if (myDeviceName != null) {
            if (currentMessage.getFromName().equals(myDeviceName)) {
                holder.message.setText(
                        String.format("ME: %s", currentMessage.getText()));
            } else {
                holder.message.setText(
                        String.format("%s: %s", currentMessage.getFromName(), currentMessage.getText()));
            }
            holder.timeSend.setText(
                    String.format("%s:%s", dateCurrentMessage.getHour(), dateCurrentMessage.getMinute()));
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
