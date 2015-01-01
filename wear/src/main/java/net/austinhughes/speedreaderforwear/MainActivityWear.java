/*
    (C) 2015 - Austin Hughes, Stefan Oswald, Nowele Rechka
    Last Modified: 2015-01-01
 */

package net.austinhughes.speedreaderforwear;

// Imports
import android.app.Activity;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

/*
    Main class for wearable side application
 */
public class MainActivityWear extends Activity {

    // private class variables
    private TextView mTextView;
    private ListenerService listener;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_activity_wear);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
            }
        });
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        listener = new ListenerService();
    }

    // Class to listen for data events
    public class ListenerService extends WearableListenerService
    {

        private GoogleApiClient mGoogleApiClient;
        private PutDataMapRequest dataMap;

        @Override
        public void onDataChanged(DataEventBuffer dataEvents)
        {
            for (DataEvent event : dataEvents)
            {
                if (event.getType() == DataEvent.TYPE_CHANGED)
                {
                    Toast.makeText(getBaseContext(), "Text received",
                            Toast.LENGTH_LONG).show();
                }
            }
        }

        @Override
        public void onCreate()
        {
            super.onCreate();
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks()
                    {
                        @Override
                        public void onConnected(Bundle connectionHint)
                        {
                            // Now you can use the Data Layer API
                            // Show feedback that we connected to the API for debug
                            Toast.makeText(getBaseContext(), "Connected to Wear Device",
                                    Toast.LENGTH_LONG).show();
                            Log.d("Wearable API", "onConnected: " + connectionHint);
                        }
                        @Override
                        public void onConnectionSuspended(int cause)
                        {
                            Log.d("Wearable API", "onConnectionSuspended: " + cause);
                        }
                    })
                    .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener()
                    {
                        @Override
                        public void onConnectionFailed(ConnectionResult result)
                        {
                            Log.d("Wearable API", "onConnectionFailed: " + result);
                        }
                    })
                            // Request access only to the Wearable API
                    .addApi(Wearable.API)
                    .build();

            mGoogleApiClient.connect();
            dataMap = PutDataMapRequest.create("/data");
        }
    }
}
