package ru.palestra.wifichat;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import ru.palestra.wifichat.databinding.ActivityChatBinding;

/**
 * Created by Dmitry on 16.11.2017.
 */

public class ChatActivity extends AppCompatActivity {
    private ActivityChatBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_chat);

    }
}
