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
    public abstract String getName();
    @Nullable
    public abstract String getNearbyKey();
    @Nullable
    public abstract String getUUID();

    public abstract boolean isOnline();

    static Builder builder() {
        return new AutoValue_Client.Builder();
    }

    @AutoValue.Builder
    abstract static class Builder {
        abstract Builder setName(String clientName);
        abstract Builder setNearbyKey(String clientNearbyKey);
        abstract Builder setUUID(String clientUUID);
        abstract Builder setOnline(boolean isOnline);
        abstract Client build();
    }

    public static Client myDevice(@NonNull String name, @NonNull String UUID) {
        return Client.builder()
                .setName(name)
                .setUUID(UUID)
                .setOnline(true)
                .build();
    }

    public static Client otherDevice(@NonNull String name, @Nullable String nearbyKey, @Nullable String UUID) {
        return Client.builder()
                .setName(name)
                .setNearbyKey(nearbyKey)
                .setUUID(UUID)  // TODO: 17.11.2017 Убрать? Передалать маппер
                .setOnline(false)
                .build();
    }

    public static Client updateUUID(@NonNull Client client, @NotNull String clientUUID) {
        return Client.builder()
                .setName(client.getName())
                .setNearbyKey(client.getNearbyKey())
                .setUUID(clientUUID)
                .setOnline(true)
                .build();
    }

    public static Client isOnline(@NonNull Client client) {
        return Client.builder()
                .setName(client.getName())
                .setNearbyKey(client.getNearbyKey())
                .setUUID(client.getUUID())
                .setOnline(true)
                .build();
    }

    public static Client isOffline(@NonNull Client client) {
        return Client.builder()
                .setName(client.getName())
                .setNearbyKey(client.getNearbyKey())
                .setUUID(client.getUUID())
                .setOnline(false)
                .build();
    }

    public static Client empty() {
        return Client.builder().setOnline(false).build();
    }

    public State getState() {
        return getName() == null && getUUID() == null ? State.EMPTY :
                getNearbyKey() == null ? State.MY_DEVICE : State.OTHER_DEVICE;
    }
}
