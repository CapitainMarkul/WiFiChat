package ru.palestra.wifichat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;
import android.widget.Toast;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import ru.palestra.wifichat.adapters.MessagesAdapter;
import ru.palestra.wifichat.data.models.mappers.MessageMapper;
import ru.palestra.wifichat.data.models.viewmodels.Client;
import ru.palestra.wifichat.data.models.viewmodels.Message;
import ru.palestra.wifichat.databinding.ActivityChatBinding;
import ru.palestra.wifichat.services.NearbyService;
import ru.palestra.wifichat.utils.ConfigIntent;
import ru.palestra.wifichat.utils.CreateUiListUtil;

/**
 * Created by Dmitry on 16.11.2017.
 */

public class ChatActivity extends AppCompatActivity {
    private ActivityChatBinding binding;

    private List<Message> uiListMessages = new ArrayList<>();

    private MessagesAdapter messagesAdapter;
    private Client myDevice;
    private Client targetDevice;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_chat);

        myDevice = App.sharedPreference().getInfoAboutMyDevice();

        targetDevice = Parcels.unwrap(getIntent().getParcelableExtra(ConfigIntent.CONNECTION_TARGET_CLIENT));

        setupTargetParams();
        setupMessagesRecyclerView();

        binding.btnSendMessage.setOnClickListener(sendMessageListener);
    }

    private void setupTargetParams() {
        binding.txtTargetName.setText(targetDevice.getName());

        if (targetDevice.isOnline()) {
            binding.imgStatusOnline.setBackground(getResources().getDrawable(R.drawable.online));
        } else {
            binding.imgStatusOnline.setBackground(getResources().getDrawable(R.drawable.offline));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(acceptConnectionToClientReceiver, new IntentFilter(ConfigIntent.ACTION_CONNECTION_INITIATED));
        registerReceiver(deliveredMessageReceiver, new IntentFilter(ConfigIntent.ACTION_DELIVERED_MESSAGE));
    }

    @Override
    protected void onStop() {
        unregisterReceiver(acceptConnectionToClientReceiver);
        unregisterReceiver(deliveredMessageReceiver);
        super.onStop();
    }

    BroadcastReceiver deliveredMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getExtras() == null) return;

            Message message =
                    (Message) intent.getSerializableExtra(ConfigIntent.MESSAGE);

            if (uiListMessages.contains(message)) return;

            // в текущем чате нужно отображать только мои сообщения и собеседника
            if (uiListMessages.contains(message) || (!message.getFromUUID().equals(myDevice.getUUID()) &&
                    !message.getFromUUID().equals(targetDevice.getUUID()))) return;

            uiListMessages.clear();
            uiListMessages.addAll(messagesAdapter.getMessages());

            messagesAdapter.updateMessages(CreateUiListUtil.createUiMessagesList(uiListMessages, message));
            scrollToBottom();
        }
    };

    BroadcastReceiver acceptConnectionToClientReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            List<Client> clients = Parcels.unwrap(intent.getParcelableExtra(ConfigIntent.UPDATED_CLIENTS));

            //Обновление статуса онлайна
            if (!clients.contains(targetDevice)) {
                targetDevice.setOnline(false);
                if (clients.contains(targetDevice)) {
                    binding.imgStatusOnline.setBackground(getResources().getDrawable(R.drawable.offline));
                } else {
                    targetDevice.setOnline(true);
                    binding.imgStatusOnline.setBackground(getResources().getDrawable(R.drawable.online));
                }
            }
        }
    };

    private View.OnClickListener sendMessageListener = view -> {
        String textMessage = binding.textMessage.getText().toString();
        if (textMessage.isEmpty()) return;

        Message sendMessage =
                Message.newMessage(myDevice.getName(), myDevice.getUUID(), targetDevice.getNearbyKey(), targetDevice.getUUID(), textMessage);

        startService(
                new Intent(this, NearbyService.class)
                        .putExtra(ConfigIntent.MESSAGE, sendMessage));

        uiListMessages.clear();
        uiListMessages.addAll(messagesAdapter.getMessages());

        messagesAdapter.updateMessages(CreateUiListUtil.createUiMessagesList(uiListMessages, sendMessage));
        scrollToBottom();
        binding.textMessage.setText("");
    };

    private void setupMessagesRecyclerView() {
        LinearLayoutManager linearLayoutManager =
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        linearLayoutManager.setStackFromEnd(true);

        binding.rvChatMessages.setLayoutManager(linearLayoutManager);

        messagesAdapter = new MessagesAdapter();
        messagesAdapter.setCurrentDevice(
                App.sharedPreference().getInfoAboutMyDevice().getUUID());
        messagesAdapter.setResendOnClick(resendOnClickListener);

        //Load Old Messages
        messagesAdapter.updateMessages(
                MessageMapper.toListMessageView(
                        App.dbClient().getAllMsgFromClient(myDevice.getUUID(), targetDevice.getUUID())));

        binding.rvChatMessages.setAdapter(messagesAdapter);
    }

    private MessagesAdapter.ResendOnClick resendOnClickListener = message -> {
        startService(
                new Intent(this, NearbyService.class)
                        .putExtra(ConfigIntent.MESSAGE, message));

        uiListMessages.remove(message);

        Message reSendMessage = Message.reSendMessage(message);
        uiListMessages.add(reSendMessage);
        CreateUiListUtil.createUiMessagesList(uiListMessages, reSendMessage);

        Toast.makeText(this, R.string.message_resend, Toast.LENGTH_SHORT).show();
    };

    private void scrollToBottom() {
        binding.rvChatMessages.scrollToPosition(messagesAdapter.getItemCount() - 1);
    }
}
