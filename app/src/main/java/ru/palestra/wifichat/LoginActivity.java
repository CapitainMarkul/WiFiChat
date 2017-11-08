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
    private final SharedPrefServiceImpl sharedPrefService = new SharedPrefServiceImpl(this);

    private Button login;
    private EditText userNickname;

    private DeviceInfo myDevice;

    private TextToSpeech mTextToSpeech;
    private boolean mIsInit;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        mTextToSpeech = new TextToSpeech(this, i -> {
            if (i == TextToSpeech.SUCCESS) {
                Locale locale = new Locale("ru_RU");
                int result = mTextToSpeech.setLanguage(locale);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    mIsInit = false;
                } else {
                    mIsInit = true;
                }
            } else {
                mIsInit = false;
            }
        });


        myDevice = sharedPrefService.getInfoAboutMyDevice();

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

                    sharedPrefService.saveInfoAboutMyDevice(myDevice);

                    gotoMainActivity();

//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                        mTextToSpeech.speak("Привет", TextToSpeech.QUEUE_FLUSH, null, "id1");
//                    }
                }
        );
    }

    private void gotoMainActivity() {
        startActivity(new Intent(this, MainActivity.class));
    }
}
