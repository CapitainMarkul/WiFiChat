package ru.palestra.wifichat.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

/**
 * Created by da.pavlov1 on 07.11.2017.
 */
@AutoValue
public abstract class DeviceInfo {
    public enum State {
        MY_DEVICE, OTHER_DEVICE, EMPTY
    }

    @Nullable
    public abstract String getClientName();

    @Nullable
    public abstract String getClientNearbyKey();

    @Nullable
    public abstract String getUUID();
//    public abstract boolean isConnected();

    public static DeviceInfo myDevice(@NonNull String name, @NonNull String UUID) {
        return new AutoValue_DeviceInfo(name, null, UUID);
    }

    public static DeviceInfo otherDevice(@NonNull String name, @NonNull String nearbyKey, @Nullable String UUID) {
        return new AutoValue_DeviceInfo(name, nearbyKey, UUID);
    }

    public static DeviceInfo empty() {
        return new AutoValue_DeviceInfo(null, null, null);
    }

    public State getState() {
        return getClientName() == null && getUUID() == null ?
                State.EMPTY :
                getClientNearbyKey() == null ?
                        State.MY_DEVICE :
                        State.OTHER_DEVICE;
    }
}
