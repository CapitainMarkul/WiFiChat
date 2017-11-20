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

import java.util.ArrayList;
import java.util.List;

import ru.palestra.wifichat.adapters.MessagesAdapter;
import ru.palestra.wifichat.data.models.mappers.MessageMapper;
import ru.palestra.wifichat.data.models.viewmodels.Client;
import ru.palestra.wifichat.data.models.viewmodels.Message;
import ru.palestra.wifichat.databinding.ActivityChatBinding;
import ru.palestra.wifichat.services.NearbyService;
import ru.palestra.wifichat.utils.ConfigIntent;

/**
 * Created by Dmitry on 16.11.2017.
 */

public class ChatActivity extends AppCompatActivity {
    private ActivityChatBinding binding;

    private List<Message> uiListMessages = new ArrayList<>();

    private MessagesAdapter messagesAdapter;
    private Client myDevice;

    private String targetNearbyId;
    private String targetName;
    private String targetUUID;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_chat);

        myDevice = App.sharedPreference().getInfoAboutMyDevice();

        targetNearbyId = getIntent().getStringExtra(ConfigIntent.CONNECTION_TARGET_ID);
        targetName = getIntent().getStringExtra(ConfigIntent.CONNECTION_TARGET_NAME);
        targetUUID = getIntent().getStringExtra(ConfigIntent.CONNECTION_TARGET_UUID);

        setTitle(targetName);

        setupMessagesRecyclerView();
        registerReceiver(deliveredMessageReceiver, new IntentFilter(ConfigIntent.ACTION_DELIVERED_MESSAGE));

        binding.btnSendMessage.setOnClickListener(sendMessageListener);
    }

    BroadcastReceiver deliveredMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getExtras() == null) return;

            Message message =
                    (Message) intent.getSerializableExtra(ConfigIntent.MESSAGE);

            uiListMessages.clear();
            uiListMessages.addAll(messagesAdapter.getMessages());

            messagesAdapter.updateMessages(createUiList(message));
            scrollToBottom();
        }
    };

    private List<Message> createUiList(Message newMessage) {
        Message[] oldMessagesArray = new Message[uiListMessages.size()];
        oldMessagesArray = uiListMessages.toArray(oldMessagesArray);

        if (!uiListMessages.contains(newMessage)) {
            for (Message oldMessage : oldMessagesArray) {
                if (oldMessage.getMsgUUID().equals(newMessage.getMsgUUID())) {
                    //Обновляем сообщение
                    int removedIndex = uiListMessages.indexOf(oldMessage);

                    uiListMessages.remove(removedIndex);
                    uiListMessages.add(removedIndex, newMessage);

                    return uiListMessages;
                }
            }
            //Если это новое сообщение
            uiListMessages.add(newMessage);
        }
        return uiListMessages;
    }

    private View.OnClickListener sendMessageListener = view -> {
        Message sendMessage =
                Message.newMessage(myDevice.getName(), myDevice.getUUID(), targetNearbyId, targetUUID, binding.textMessage.getText().toString());

        startService(
                new Intent(this, NearbyService.class)
                        .putExtra(ConfigIntent.MESSAGE, sendMessage));

        uiListMessages.clear();
        uiListMessages.addAll(messagesAdapter.getMessages());

        messagesAdapter.updateMessages(createUiList(sendMessage));
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
                App.sharedPreference().getInfoAboutMyDevice().getName());

        //Load Old Messages
        messagesAdapter.updateMessages(
                MessageMapper.toListMessageView(
                        App.dbClient().getAllMsgFromClient(myDevice.getUUID(), targetUUID)));

        binding.rvChatMessages.setAdapter(messagesAdapter);
    }

    private void scrollToBottom() {
        binding.rvChatMessages.scrollToPosition(messagesAdapter.getItemCount() - 1);
    }
}
