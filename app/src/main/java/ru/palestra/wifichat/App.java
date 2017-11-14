package ru.palestra.wifichat;

import android.app.Application;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.nearby.Nearby;
import com.jakewharton.threetenabp.AndroidThreeTen;

import ru.palestra.wifichat.services.SharedPrefServiceImpl;

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
}
