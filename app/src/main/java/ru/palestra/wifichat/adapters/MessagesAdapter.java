package ru.palestra.wifichat.adapters;

import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.threeten.bp.LocalDateTime;

import java.util.ArrayList;
import java.util.List;

import ru.palestra.wifichat.R;
import ru.palestra.wifichat.adapters.diffutil.MessageDiffUtil;
import ru.palestra.wifichat.data.models.viewmodels.Message;
import ru.palestra.wifichat.databinding.ItemMessageBinding;
import ru.palestra.wifichat.utils.TimeUtils;

/**
 * Created by Dmitry on 01.11.2017.
 */

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.ViewHolder> {
    private List<Message> messages = new ArrayList<>();
    private String myDeviceName;

    public void setCurrentDevice(@NonNull String myDeviceName) {
        this.myDeviceName = myDeviceName;
    }

    public void updateMessages(List<Message> messages) {
        DiffUtil.DiffResult diffResult =
                DiffUtil.calculateDiff(new MessageDiffUtil(this.messages, messages));
        this.messages.clear();
        this.messages.addAll(messages);
        diffResult.dispatchUpdatesTo(this);
    }
//
//    private void sortMessages() {
//        Collections.sort(messages, (message1, message2) -> {
//            LocalDateTime dateMessage1 = TimeUtils.longToLocalDateTime(message1.getTimeSend());
//            LocalDateTime dateMessage2 = TimeUtils.longToLocalDateTime(message2.getTimeSend());
//
//            if (dateMessage1.isAfter(dateMessage2)) {
//                return 1;
//            } else if (dateMessage1.isEqual(dateMessage2)) {
//                return 0;
//            } else {
//                return -1;
//            }
//        });
//    }

    public List<Message> getMessages(){
        return messages;
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

        holder.binding.txtTimeSend.setText(
                String.format("%s:%s", dateCurrentMessage.getHour(), dateCurrentMessage.getMinute()));

        if (currentMessage.getFromName().equals(myDeviceName)) {
            holder.binding.txtTextMessage.setText(
                    String.format("ME: %s", currentMessage.getText()));

            if (currentMessage.isDelivered()) {
                holder.binding.btnStatusMessage.setVisibility(View.INVISIBLE);
            } else {
                holder.binding.btnStatusMessage.setVisibility(View.VISIBLE);
            }
        } else {
            holder.binding.txtTextMessage.setText(
                    String.format("%s: %s", currentMessage.getFromName(), currentMessage.getText()));
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemMessageBinding binding;

        public ViewHolder(View itemView) {
            super(itemView);
            binding = DataBindingUtil.bind(itemView);
        }
    }
}
