package ru.palestra.wifichat;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.nearby.Nearby;
import com.jakewharton.threetenabp.AndroidThreeTen;

import ru.palestra.wifichat.data.models.daomodels.DaoMaster;
import ru.palestra.wifichat.data.models.daomodels.DaoSession;
import ru.palestra.wifichat.domain.db.DbClient;
import ru.palestra.wifichat.services.SharedPrefServiceImpl;
import ru.palestra.wifichat.utils.Logger;

/**
 * Created by da.pavlov1 on 10.11.2017.
 */

public class App extends Application {
    private static SharedPrefServiceImpl sharedPrefInstance;
    private static GoogleApiClient googleApiClientInstance;
    private static DbClient dbClientInstance;

    private DaoSession daoSession;

    @Override
    public void onCreate() {
        super.onCreate();
        AndroidThreeTen.init(this);

        initDaoSession();
        initDbClient();
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

    private void initDaoSession() {
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(this, "WaiterDb", null);
        SQLiteDatabase db = helper.getWritableDatabase();
        DaoMaster daoMaster = new DaoMaster(db);
        daoSession = daoMaster.newSession();
    }

    private void initDbClient() {
        dbClientInstance = new DbClient(daoSession);
    }

    public static GoogleApiClient googleApiClient() {
        return googleApiClientInstance;
    }

    public static SharedPrefServiceImpl sharedPreference() {
        return sharedPrefInstance;
    }

    public static DbClient dbClient() {
        return dbClientInstance;
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
