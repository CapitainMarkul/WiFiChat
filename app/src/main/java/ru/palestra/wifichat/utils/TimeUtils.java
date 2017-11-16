package ru.palestra.wifichat.utils;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneId;

/**
 * Created by da.pavlov1 on 16.11.2017.
 */

public class TimeUtils {
    private TimeUtils() {

    }

    public static long timeNowLong() {
        return LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public static LocalDateTime timeNowLocalDateTime() {
        return LocalDateTime.now(ZoneId.systemDefault());
    }


    public static LocalDateTime longToLocalDateTime(long longTime) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(longTime), ZoneId.systemDefault());
    }
}
