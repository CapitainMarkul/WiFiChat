package ru.palestra.wifichat.adapters;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import org.threeten.bp.LocalDateTime;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
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
    public interface ResendOnClick {
        void onClick(Message message);
    }

    private ResendOnClick resendOnClick;

    private List<Message> messages = new ArrayList<>();
    private String myDeviceUUID;

    public void setCurrentDevice(@NonNull String myDeviceUUID) {
        this.myDeviceUUID = myDeviceUUID;
    }

    public void setResendOnClick(@NonNull ResendOnClick resendOnClick) {
        this.resendOnClick = resendOnClick;
    }

    public void updateMessages(List<Message> messages) {
        DiffUtil.DiffResult diffResult =
                DiffUtil.calculateDiff(new MessageDiffUtil(this.messages, messages));

        this.messages.clear();
        this.messages.addAll(messages);

        sortMessages();

        diffResult.dispatchUpdatesTo(this);
    }

    public List<Message> getMessages() {
        return messages;
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

        DecimalFormat myFormatter = new DecimalFormat("00");
        holder.binding.txtTimeSend.setText(
                String.format("%s:%s", myFormatter.format(dateCurrentMessage.getHour()), myFormatter.format(dateCurrentMessage.getMinute())));

//        LinearLayout.LayoutParams paramsMsg =
//                new LinearLayout.LayoutParams(holder.binding.container.getLayoutParams());

        if (currentMessage.getFromUUID().equals(myDeviceUUID)) {
//            paramsMsg.layoutGravity = Gravity.END;

            setupMyMessage(currentMessage, holder);
        } else {
//            paramsMsg.gravity = Gravity.START;

            setNotMyMessage(currentMessage, holder);
        }

//        holder.binding.messageContainer.setLayoutParams(paramsMsg);
    }

    private void setupMyMessage(Message currentMessage, ViewHolder holder) {
        holder.itemView.setBackground(
                holder.context.getResources().getDrawable(R.drawable.background_my_message));

        holder.binding.txtTextMessage.setText(
                String.format("ME: %s", currentMessage.getText()));

        if (currentMessage.isDelivered()) {
            holder.binding.btnStatusMessage.setVisibility(View.GONE);
        } else {
            holder.binding.btnStatusMessage.setVisibility(View.VISIBLE);

            if (resendOnClick != null) {
                holder.binding.btnStatusMessage.setOnClickListener(view -> resendOnClick.onClick(currentMessage));
            }
        }
    }

    private void setNotMyMessage(Message currentMessage, ViewHolder holder) {
        holder.itemView.setBackground(
                holder.context.getResources().getDrawable(R.drawable.background_not_my_message));

        holder.binding.txtTextMessage.setText(
                String.format("%s: %s", currentMessage.getFromName(), currentMessage.getText()));

        holder.binding.btnStatusMessage.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private Context context;
        private ItemMessageBinding binding;

        public ViewHolder(View itemView) {
            super(itemView);
            context = itemView.getContext();
            binding = DataBindingUtil.bind(itemView);
        }
    }
}
