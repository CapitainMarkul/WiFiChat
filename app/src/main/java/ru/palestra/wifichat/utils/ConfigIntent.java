package ru.palestra.wifichat.utils;

/**
 * Created by da.pavlov1 on 13.11.2017.
 */

public class ConfigIntent {
    private ConfigIntent() {
    }

    public final static String ACTION_CONNECTION_INITIATED = "CONNECTION_INITIATED";
    public final static String ACTION_DISCOVERY = "ACTION_DISCOVERY";
    public final static String ACTION_DELIVERED_MESSAGE = "DELIVERED_MESSAGE";

    public final static String MESSAGE = "MESSAGE";
    public final static String UPDATED_CLIENTS = "UPDATED_CLIENTS";

    public final static String STATUS_DISCOVERY = "STATUS_DISCOVERY";

    public final static String CONNECTION_TARGET_CLIENT = "CONNECTION_TARGET_CLIENT";
}
