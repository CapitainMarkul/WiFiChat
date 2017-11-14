package ru.palestra.wifichat;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.EditText;

import java.util.Locale;
import java.util.UUID;

import ru.palestra.wifichat.model.DeviceInfo;
import ru.palestra.wifichat.services.SharedPrefServiceImpl;

/**
 * Created by da.pavlov1 on 07.11.2017.
 */

public class LoginActivity extends AppCompatActivity {
    private Button login;
    private EditText userNickname;

    private DeviceInfo myDevice;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        myDevice = App.sharedPreference().getInfoAboutMyDevice();

        if (myDevice.getState() == DeviceInfo.State.MY_DEVICE) {
            gotoMainActivity();
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        userNickname = findViewById(R.id.et_your_nickname);

        login = findViewById(R.id.btn_login);
        login.setOnClickListener(view -> {
                    myDevice = DeviceInfo.myDevice(
                            userNickname.getText().toString(),
                            UUID.randomUUID().toString());

                    App.sharedPreference().saveInfoAboutMyDevice(myDevice);

                    gotoMainActivity();
                }
        );
    }

    private void gotoMainActivity() {
        startActivity(new Intent(this, MainActivity.class));
    }
}
