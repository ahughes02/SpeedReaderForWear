/*
    (C) 2015 - Austin Hughes, Stefan Oswald, Nowele Rechka
    Last Modified: 2015-01-01
 */

package net.austinhughes.speedreaderforwear;

// Imports
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.util.List;

/*
    Main class for wearable side application
 */
public class MainActivityWear extends Activity implements ConnectionCallbacks, DataApi.DataListener, OnConnectionFailedListener
{
    // private class variables
    private TextView mTextView;
    private GoogleApiClient mGoogleApiClient;
    private  Handler mHandler;
    private static final String TAG = "DataListenerService";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        setContentView(R.layout.activity_main_activity_wear);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener()
        {
            @Override
            public void onLayoutInflated(WatchViewStub stub)
            {
                mTextView = (TextView) stub.findViewById(R.id.text);
            }
        });

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        Log.d("Wearable DataListenerService", "onCreate");
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    protected  void onPause()
    {
        super.onPause();
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(Bundle connectionHint)
    {
        Log.d(TAG, "onConnected(): connected to Google API client");
        Wearable.DataApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int cause)
    {
        Log.d(TAG, "onConnectionSuspended(): Connection to Google API client was suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result)
    {
        Log.e(TAG, "onConnectionFailed(): Failed to connect with result: " + result);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents)
    {
        Log.d("Wearable DataListenerService", "onDataChanged" + dataEvents);

        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
        dataEvents.close();

        for (DataEvent event : events)
        {
            if (event.getType() == DataEvent.TYPE_CHANGED)
            {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                String text = dataMapItem.getDataMap().toString();
                Log.d("DataItem", text);
                mTextView.setText(text);
            }
        }
    }
}
