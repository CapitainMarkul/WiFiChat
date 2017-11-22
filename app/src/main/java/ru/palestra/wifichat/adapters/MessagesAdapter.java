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
import android.widget.FrameLayout;

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
        sortMessages(messages);

        DiffUtil.DiffResult diffResult =
                DiffUtil.calculateDiff(new MessageDiffUtil(this.messages, messages));

        this.messages.clear();
        this.messages.addAll(messages);

        diffResult.dispatchUpdatesTo(this);
    }

    public List<Message> getMessages() {
        return messages;
    }

    private void sortMessages(List<Message> sortedList) {
        Collections.sort(sortedList, (message1, message2) -> {
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
        DecimalFormat timePattern = new DecimalFormat("00");

        FrameLayout.LayoutParams paramsMsg =
                new FrameLayout.LayoutParams(holder.binding.containerMessage.getLayoutParams());

        if (currentMessage.getFromUUID().equals(myDeviceUUID)) {
            paramsMsg.gravity = Gravity.START;

            setupMyMessage(currentMessage, timePattern, dateCurrentMessage, holder);
        } else {
            paramsMsg.gravity = Gravity.END;

            setNotMyMessage(currentMessage, timePattern, dateCurrentMessage, holder);
        }

        holder.binding.containerMessage.setLayoutParams(paramsMsg);
    }

    private void setupMyMessage(Message currentMessage, DecimalFormat timeFormat, LocalDateTime dateCurrentMessage, ViewHolder holder) {
        holder.binding.spacerLeft.setVisibility(View.VISIBLE);
        holder.binding.txtTimeSendLeft.setVisibility(View.VISIBLE);
        holder.binding.btnStatusMessage.setVisibility(View.VISIBLE);

        holder.binding.spacerRight.setVisibility(View.GONE);
        holder.binding.txtTimeSendRight.setVisibility(View.GONE);

        holder.binding.txtTimeSendLeft.setText(
                String.format("%s:%s", timeFormat.format(dateCurrentMessage.getHour()), timeFormat.format(dateCurrentMessage.getMinute())));

        holder.binding.containerMessage.setBackground(
                holder.context.getResources().getDrawable(R.drawable.background_my_message));

        holder.binding.txtTextMessage.setText(
                String.format("%s", currentMessage.getText()));


        if (currentMessage.isDelivered()) {
            holder.binding.btnStatusMessage.setVisibility(View.GONE);
        } else {
            holder.binding.btnStatusMessage.setVisibility(View.VISIBLE);

            if (resendOnClick != null) {
                holder.binding.btnStatusMessage.setOnClickListener(view -> resendOnClick.onClick(currentMessage));
            }
        }
    }

    private void setNotMyMessage(Message currentMessage, DecimalFormat timeFormat, LocalDateTime dateCurrentMessage, ViewHolder holder) {
        holder.binding.spacerRight.setVisibility(View.VISIBLE);
        holder.binding.txtTimeSendRight.setVisibility(View.VISIBLE);

        holder.binding.spacerLeft.setVisibility(View.GONE);
        holder.binding.txtTimeSendLeft.setVisibility(View.GONE);
        holder.binding.btnStatusMessage.setVisibility(View.GONE);

        holder.binding.txtTimeSendRight.setText(
                String.format("%s:%s", timeFormat.format(dateCurrentMessage.getHour()), timeFormat.format(dateCurrentMessage.getMinute())));

        holder.binding.containerMessage.setBackground(
                holder.context.getResources().getDrawable(R.drawable.background_not_my_message));

        holder.binding.txtTextMessage.setText(
                String.format("%s", currentMessage.getText()));

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
