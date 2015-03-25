/*
    (C) 2015 - Austin Hughes, Stefan Oswald, Nowele Rechka
    Last Modified: 2015-02-12
 */

package net.austinhughes.speedreaderforwear;

// Imports
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/*
    Main class for wearable side application
 */
public class MainActivityWear extends Activity implements ConnectionCallbacks, DataApi.DataListener, OnConnectionFailedListener
{
    private TextView mTextView; // Stores the main app text view
    private GoogleApiClient mGoogleApiClient; // The Google API for talking to the phone
    private static final String TAG = "MainActivityWear"; // Tag for log

    // Used to "speed read" text data
    private String[] currentData;
    private int iterator;
    private int interval = 250; // speed to update at in ms

    // Private data store
    private final String FILENAME = "stored_data";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Setup main view and set mTextView to the main text display
        setContentView(R.layout.activity_main_activity_wear);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener()
        {
            @Override
            public void onLayoutInflated(WatchViewStub stub)
            {
                mTextView = (TextView) stub.findViewById(R.id.text);

                // Read in previous data from file on boot
                try
                {
                    FileInputStream fis = openFileInput(FILENAME);
                    BufferedReader br = new BufferedReader(new InputStreamReader(fis));

                    String text = br.readLine();
                    currentData = text.split(" ");
                    setText("Tap to read data from file.");

                    Log.d(TAG, "Read from file " + FILENAME + " successfully! Text: " + text);
                }
                catch (Exception e)
                {
                    Log.d(TAG, "Unable to read saved data: " + e.toString());
                }
            }
        });

        // Connect to the Google API
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        Log.d(TAG, "onCreate()");
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        // Reconnect to Google API
        if(!mGoogleApiClient.isConnected() && !mGoogleApiClient.isConnecting())
        {
            mGoogleApiClient.connect();
        }

        Log.d(TAG, "onResume()");
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        // Disconnect from the listener and the Google API
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();

        Log.d(TAG, "onPause()");
    }

    @Override
    public void onConnected(Bundle connectionHint)
    {
        // Reconnect to the data listener
        Wearable.DataApi.addListener(mGoogleApiClient, this);

        Log.d(TAG, "onConnected(): connected to Google API client");
    }

    @Override
    public void onConnectionSuspended(int cause)
    {
        Log.d(TAG, "onConnectionSuspended(): Connection to Google API client was suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result)
    {
        Log.e(TAG, "onConnectionFailed(): Failed to connect to Google API client with result: " + result);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents)
    {
        Log.d(TAG, "onDataChanged(DataEventBuffer dataEvents): " + dataEvents);

        // Get the list of events
        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
        dataEvents.close();

        // Loop through the data events
        for (DataEvent event : events)
        {
            // We only care about data changed events
            if (event.getType() == DataEvent.TYPE_CHANGED)
            {
                // extract the data item
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                final String text = dataMapItem.getDataMap().get("editTextValue");
                Log.d(TAG, "DataItem: " + text);

                // write the data out to a file
                try
                {
                    // Make sure file is clean
                    deleteFile(FILENAME);

                    // Write to the file
                    FileOutputStream fos = openFileOutput(FILENAME, Context.MODE_PRIVATE);
                    fos.write(text.getBytes());
                    fos.close();
                }
                catch(IOException e)
                {
                    Log.d(TAG, "IO Exception: " + e.toString());
                }

                // Set up speed reading
                currentData = text.split(" ");
                setText("New Data Received. Tap to Read.");
            }
        }
    }

    // Calls iterate text at a fixed interval to allow a user to "speed read"
    public void spreed() throws InterruptedException
    {
        // reset iterator
        iterator = 0;

        // Update text at set interval TODO: Make interval configurable
        new Timer().scheduleAtFixedRate(new TimerTask()
        {
            @Override
            public void run()
            {
                if(!iterateText())
                {
                    setText("Tap to read again");
                    this.cancel();
                }
            }
        }, 0, interval);
    }

    // Helper method for spreed, updates the text by stepping though the currentData array
    private Boolean iterateText()
    {
        if(iterator < currentData.length)
        {
            setText(currentData[iterator]);
            iterator++;
            return true;
        }

        if(iterator == currentData.length)
        {
            // store last text for a bit
            iterator++;
            return true;
        }

        return false;
    }

    // Helper method to set new text in mTextView
    public void setText(String input)
    {
        final String text = input;

        // runs a text update on the UI thread
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                mTextView.setText(text);
            }
        });
    }

    // Gets called whenever the text is pressed
    public void onTextPressed(View v) throws InterruptedException
    {
        spreed();
    }
}
