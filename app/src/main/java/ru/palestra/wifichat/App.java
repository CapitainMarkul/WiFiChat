package ru.palestra.wifichat;

import android.app.Application;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.nearby.Nearby;
import com.jakewharton.threetenabp.AndroidThreeTen;

import ru.palestra.wifichat.services.SharedPrefServiceImpl;
import ru.palestra.wifichat.utils.Logger;

/**
 * Created by da.pavlov1 on 10.11.2017.
 */

public class App extends Application {
    private static SharedPrefServiceImpl sharedPrefInstance;
    private static GoogleApiClient googleApiClientInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        AndroidThreeTen.init(this);

        initGoogleClient();
        initSharedPreference();
    }

    private void initGoogleClient() {
        googleApiClientInstance = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(connectionCallbacks)
                .addOnConnectionFailedListener(connectionFailedListener)
                .addApi(Nearby.CONNECTIONS_API)
                .build();

        googleApiClientInstance.connect();
    }

    private void initSharedPreference() {
        sharedPrefInstance = new SharedPrefServiceImpl(this);
    }

    public static GoogleApiClient googleApiClient() {
        return googleApiClientInstance;
    }

    public static SharedPrefServiceImpl sharedPreference() {
        return sharedPrefInstance;
    }

    GoogleApiClient.ConnectionCallbacks connectionCallbacks = new GoogleApiClient.ConnectionCallbacks() {
        @Override
        public void onConnected(@Nullable Bundle bundle) {
            Logger.debugLog("connectionCallbacks: onConnected");


        }

        @Override
        public void onConnectionSuspended(int i) {
            Logger.debugLog("connectionCallbacks: onConnectionSuspended " + i);
        }
    };

    /**
     * GoogleApiClient.OnConnectionFailedListener
     * Неудачи подключения
     */
    GoogleApiClient.OnConnectionFailedListener connectionFailedListener = connectionResult -> {

    };
}
