package ru.palestra.wifichat.utils;

import android.util.Log;

/**
 * Created by Dmitry on 13.11.2017.
 */

public class Logger {
    private final static String TAG = Logger.class.getSimpleName();

    private Logger() {

    }

    public static void debugLog(String textLog) {
        Log.d(TAG, textLog);
    }

    public static void errorLog(String textLog) {
        Log.e(TAG, textLog);
    }
}
