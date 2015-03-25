/*
    (C) 2015 - Austin Hughes, Stefan Oswald, Nowele Rechka
    Last Modified: 2015-02-12
 */

package net.austinhughes.speedreaderforwear;

// Imports
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/*
    Main class for phone side application
 */
public class MainActivity extends ActionBarActivity
{
    // Private class variables
    private GoogleApiClient mGoogleApiClient;
    private PutDataMapRequest dataMap;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Generate the API client for talking to the Wear device
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks()
                {
                    @Override
                    public void onConnected(Bundle connectionHint)
                    {
                        // Now you can use the Data Layer API
                        // Show feedback that we connected to the API for debug
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

        // Create the data map so we can sync data to Wear
        dataMap = PutDataMapRequest.create("/data");
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop()
    {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onRSSButtonPressed(View v)
    {
        try
        {
            URL url = new URL("http://feeds.pcworld.com/pcworld/latestnews");
            RSSReader rss = new RSSReader();
            List list = rss.LoadRSSHeadlines(url);

            Object[] arr = list.toArray();

            Log.d("Object", arr[0].toString());
        }
        catch(MalformedURLException e)
        {
            Log.d("Error", e.toString());
        }
    }

    // Gets called whenever the send button is pressed
    public void onSendButtonPressed(View v)
    {
        // Grab the text from the EditText text field
        EditText mEdit = (EditText)findViewById(R.id.editText);

        // Clear the data map then put the text into it, the Google API client will auto sync it to Wear
        dataMap.getDataMap().clear();
        dataMap.getDataMap().putString("editTextValue", mEdit.getText().toString());
        PutDataRequest request = dataMap.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi
                .putDataItem(mGoogleApiClient, request);
    }
}