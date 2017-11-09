package ru.palestra.wifichat.model;

import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;

/**
 * Created by da.pavlov1 on 07.11.2017.
 */

public class DeviceInfo {
    public enum State {
        MY_DEVICE, OTHER_DEVICE, EMPTY
    }

    private String UUID;
    private final String clientName;
    private final String clientNearbyKey;
    private boolean isConnected;

    public static DeviceInfo myDevice(@NonNull String name, @NonNull String UUID) {
        return new DeviceInfo(name, null, UUID);
    }

    public static DeviceInfo otherDevice(@NonNull String name, @NonNull String nearbyKey, @Nullable String UUID) {
        return new DeviceInfo(name, nearbyKey, UUID);
    }

    public static DeviceInfo empty() {
        return new DeviceInfo(null, null, null);
    }

    private DeviceInfo(String name, @Nullable String nearbyKey, @Nullable String UUID) {
        this.clientName = name;
        this.clientNearbyKey = nearbyKey;
        this.UUID = UUID;
    }

    public String getClientNearbyKey() {
        return clientNearbyKey;
    }

    public String getClientName() {
        return clientName;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    public State getState() {
        return clientName == null && UUID == null ?
                State.EMPTY :
                clientNearbyKey == null ?
                        State.MY_DEVICE :
                        State.OTHER_DEVICE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DeviceInfo deviceInfo = (DeviceInfo) o;

        if (getClientName() != null ? !getClientName().equals(deviceInfo.getClientName()) : deviceInfo.getClientName() != null)
            return false;
        if (getClientNearbyKey() != null ? !getClientNearbyKey().equals(deviceInfo.getClientNearbyKey()) : deviceInfo.getClientNearbyKey() != null)
            return false;
        if (getUUID() != null ? !getUUID().equals(deviceInfo.getUUID()) : deviceInfo.getUUID() != null)
            return false;

        return getState() != null ? getState().equals(deviceInfo.getState()) : deviceInfo.getState() == null;
    }

    @Override
    public int hashCode() {
        int result = getClientName() != null ? getClientName().hashCode() : 0;
        result = 31 * result + (getClientNearbyKey() != null ? getClientNearbyKey().hashCode() : 0);
        result = 31 * result + (getUUID() != null ? getUUID().hashCode() : 0);
        result = 31 * result + (getState() != null ? getState().hashCode() : 0);

        return result;
    }
}
