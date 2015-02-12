/*
    (C) 2015 - Austin Hughes, Stefan Oswald, Nowele Rechka
    Last Modified: 2015-02-12
 */

package net.austinhughes.speedreaderforwear;

import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.concurrent.TimeUnit;

/*
    Listener for data layer events in the Google Wearable API
 */
public class DataListenerService extends WearableListenerService
{
    private static final String TAG = "DataListenerService";

    GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate()
    {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents)
    {
        Log.d(TAG, "onDataChanged: " + dataEvents);

        if(!mGoogleApiClient.isConnected())
        {
            ConnectionResult connectionResult = mGoogleApiClient
                    .blockingConnect(30, TimeUnit.SECONDS);
            if (!connectionResult.isSuccess())
            {
                Log.e(TAG, "Failed to connect to GoogleApiClient.");
                return;
            }
        }
    }
}
