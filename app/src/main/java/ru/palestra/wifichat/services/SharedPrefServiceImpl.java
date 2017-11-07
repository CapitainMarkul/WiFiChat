package ru.palestra.wifichat.services;

import android.content.Context;
import android.content.SharedPreferences;

import ru.palestra.wifichat.model.DeviceInfo;

/**
 * Created by da.pavlov1 on 07.11.2017.
 */
// TODO: 07.11.2017 VIPER ?
public class SharedPrefServiceImpl {
    private final Context context;

    private static final String PREF_FILE_KEY = "pref_offline_chat";
    private static final String PREF_KEY_NAME_MY_DEVICE = "name_device";
    private static final String PREF_KEY_UUID_MY_DEVICE = "UUID_device";

    public SharedPrefServiceImpl(Context context) {
        this.context = context;
    }

    public void saveInfoAboutMyDevice(DeviceInfo myDevice) {
        getPrefFile().edit()
                .putString(PREF_KEY_NAME_MY_DEVICE, myDevice.getClientName())
                .putString(PREF_KEY_UUID_MY_DEVICE, myDevice.getUUID())
                .apply();
    }

    public DeviceInfo getInfoAboutMyDevice() {
        String name = getPrefFile().getString(PREF_KEY_NAME_MY_DEVICE, null);
        String UUID = getPrefFile().getString(PREF_KEY_UUID_MY_DEVICE, null);

        return name != null && UUID != null ?
                DeviceInfo.myDevice(name, UUID) : DeviceInfo.empty();
    }

    private SharedPreferences getPrefFile() {
        return context.getSharedPreferences(
                PREF_FILE_KEY, Context.MODE_PRIVATE);
    }
}
