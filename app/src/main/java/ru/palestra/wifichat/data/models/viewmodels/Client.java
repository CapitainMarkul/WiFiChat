package ru.palestra.wifichat.data.models.viewmodels;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import org.greenrobot.greendao.annotation.NotNull;

/**
 * Created by da.pavlov1 on 07.11.2017.
 */
@AutoValue
public abstract class Client {
    public enum State {
        MY_DEVICE, OTHER_DEVICE, EMPTY
    }

    @Nullable
    public abstract String getClientName();
    @Nullable
    public abstract String getClientNearbyKey();
    @Nullable
    public abstract String getUUID();

    public abstract boolean isOnline();

    static Builder builder() {
        return new AutoValue_Client.Builder();
    }

    @AutoValue.Builder
    abstract static class Builder {
        abstract Builder setClientName(String clientName);
        abstract Builder setClientNearbyKey(String clientNearbyKey);
        abstract Builder setUUID(String clientUUID);
        abstract Builder setOnline(boolean isOnline);
        abstract Client build();
    }

    public static Client myDevice(@NonNull String name, @NonNull String UUID) {
        return Client.builder()
                .setClientName(name)
                .setUUID(UUID)
                .setOnline(true)
                .build();
    }

    public static Client otherDevice(@NonNull String name, @Nullable String nearbyKey, @Nullable String UUID) {
        return Client.builder()
                .setClientName(name)
                .setClientNearbyKey(nearbyKey)
                .setUUID(UUID)  // TODO: 17.11.2017 Убрать? Передалать маппер
                .build();
    }

    public static Client updateUUID(@NonNull Client client, @NotNull String clientUUID) {
        return Client.builder()
                .setClientName(client.getClientName())
                .setClientNearbyKey(client.getClientNearbyKey())
                .setUUID(clientUUID)
                .setOnline(true)
                .build();
    }

    public static Client empty() {
        return Client.builder().build();
    }

    public State getState() {
        return getClientName() == null && getUUID() == null ?
                State.EMPTY :
                getClientNearbyKey() == null ?
                        State.MY_DEVICE :
                        State.OTHER_DEVICE;
    }
}
