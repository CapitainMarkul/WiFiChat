package ru.palestra.wifichat.presentation.adapters;

import static ru.palestra.wifichat.services.NearbyService.PINE_CLIENT_UUID;

import android.content.Context;

import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

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
import ru.palestra.wifichat.presentation.adapters.diffutil.MessageDiffUtil;
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

        boolean msgFromPine = currentMessage.getFromUUID().equals(PINE_CLIENT_UUID);
        if (msgFromPine || currentMessage.getFromUUID().equals(myDeviceUUID)) {
            paramsMsg.gravity = Gravity.END;

            setupMyMessage(currentMessage, msgFromPine, timePattern, dateCurrentMessage, holder);
        } else {
            paramsMsg.gravity = Gravity.START;

            setNotMyMessage(currentMessage, timePattern, dateCurrentMessage, holder);
        }

        holder.binding.containerMessage.setLayoutParams(paramsMsg);
    }

    private void setupMyMessage(
            Message currentMessage,
            boolean msgFromPine,
            DecimalFormat timeFormat,
            LocalDateTime dateCurrentMessage,
            ViewHolder holder
    ) {
        holder.binding.txtSenderNickname.setVisibility(View.GONE);

        holder.binding.txtTimeSendRight.setText(
                String.format("%s:%s", timeFormat.format(dateCurrentMessage.getHour()), timeFormat.format(dateCurrentMessage.getMinute())));

        holder.binding.containerMessage.setBackground(
                ContextCompat.getDrawable(holder.context, R.drawable.background_my_message));

        holder.binding.txtTextMessage.setText(
                String.format("%s", currentMessage.getText()));
    }

    private void setNotMyMessage(Message currentMessage, DecimalFormat timeFormat, LocalDateTime dateCurrentMessage, ViewHolder holder) {
        holder.binding.txtSenderNickname.setVisibility(View.VISIBLE);
        holder.binding.txtSenderNickname.setText(currentMessage.getFromName());

        holder.binding.txtTimeSendRight.setText(
                String.format("%s:%s", timeFormat.format(dateCurrentMessage.getHour()), timeFormat.format(dateCurrentMessage.getMinute())));

        holder.binding.containerMessage.setBackground(
                ContextCompat.getDrawable(holder.context, R.drawable.background_not_my_message));

        holder.binding.txtTextMessage.setText(
                String.format("%s", currentMessage.getText()));
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
