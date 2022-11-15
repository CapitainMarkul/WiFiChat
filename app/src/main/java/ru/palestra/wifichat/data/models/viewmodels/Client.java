package ru.palestra.wifichat.data.models.viewmodels;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.parceler.Parcel;

/**
 * Created by da.pavlov1 on 07.11.2017.
 */

@Parcel
public class Client {
    public enum State {
        MY_DEVICE, OTHER_DEVICE, EMPTY
    }

    private String name;
    private String nearbyKey;
    private String UUID;
    private Boolean isOnline;

    public Client() {

    }

    public static Client myDevice(@NonNull String name, @NonNull String UUID) {
        return new Client()
                .setName(name)
                .setUUID(UUID)
                .setOnline(true);
    }

    public static Client otherDevice(@NonNull String name, @Nullable String nearbyKey, @Nullable String UUID) {
        return new Client()
                .setName(name)
                .setNearbyKey(nearbyKey)
                .setUUID(UUID)
                .setOnline(false);
    }

    public static Client empty() {
        return new Client();
    }

    public State getState() {
        return getName() == null && getUUID() == null ? State.EMPTY :
                getNearbyKey() == null ? State.MY_DEVICE : State.OTHER_DEVICE;
    }

    public String getName() {
        return name;
    }

    public Client setName(String name) {
        this.name = name;
        return this;
    }

    public String getNearbyKey() {
        return nearbyKey;
    }

    public Client setNearbyKey(String nearbyKey) {
        this.nearbyKey = nearbyKey;
        return this;
    }

    public String getUUID() {
        return UUID;
    }

    public Client setUUID(String UUID) {
        this.UUID = UUID;
        return this;
    }

    public Boolean isOnline() {
        return isOnline;
    }

    public Client setOnline(Boolean isOnline) {
        this.isOnline = isOnline;
        return this;
    }

    @Override
    public String toString() {
        return "Client{"
                + "name=" + name + ", "
                + "nearbyKey=" + nearbyKey + ", "
                + "UUID=" + UUID + ", "
                + "isOnline=" + isOnline
                + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof Client) {
            Client that = (Client) o;
            return ((this.name == null) ? (that.getName() == null) : this.name.equals(that.getName()))
                    && ((this.nearbyKey == null) ? (that.getNearbyKey() == null) : this.nearbyKey.equals(that.getNearbyKey()))
                    && ((this.UUID == null) ? (that.getUUID() == null) : this.UUID.equals(that.getUUID()))
                    && (this.isOnline == that.isOnline());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h = 1;
        h *= 1000003;
        h ^= (name == null) ? 0 : this.name.hashCode();
        h *= 1000003;
        h ^= (nearbyKey == null) ? 0 : this.nearbyKey.hashCode();
        h *= 1000003;
        h ^= (UUID == null) ? 0 : this.UUID.hashCode();
        h *= 1000003;
        h ^= this.isOnline ? 1231 : 1237;
        return h;
    }
}
