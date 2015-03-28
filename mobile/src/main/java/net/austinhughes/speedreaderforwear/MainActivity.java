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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;

/*
    Main class for phone side application
 */
public class MainActivity extends ActionBarActivity
{
    // Private class variables
    private GoogleApiClient mGoogleApiClient;
    private PutDataMapRequest dataMap;

    private static final String TAG = "MainActivity"; // Tag for log
    private final String HEADLINES_FILENAME = "rss_headlines";
    private final String DESCRIPTIONS_FILENAME = "rss_descriptions";

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
                        Log.d(TAG, "onConnected: " + connectionHint);
                    }
                    @Override
                    public void onConnectionSuspended(int cause)
                    {
                        Log.d(TAG, "onConnectionSuspended: " + cause);
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener()
                {
                    @Override
                    public void onConnectionFailed(ConnectionResult result)
                    {
                        Log.d(TAG, "onConnectionFailed: " + result);
                    }
                })
                // Request access only to the Wearable API
                .addApi(Wearable.API)
                .build();

        // Create the data map so we can sync data to Wear
        dataMap = PutDataMapRequest.create("/data");

        try
        {
            URL url = new URL("http://www.anandtech.com/rss/");
            new RSSReader(getApplicationContext()).execute(url);
        }
        catch (Exception e)
        {
            Log.d(TAG, "Exception " + e.toString());
        }
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
            FileInputStream fis = openFileInput(DESCRIPTIONS_FILENAME);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));

            String text = br.readLine();
            text = br.readLine();

            Log.d(TAG, text);

            if(text != null)
            {
                EditText mEdit = (EditText) findViewById(R.id.editText);

                mEdit.setText(text);
            }

            Log.d(TAG, text);
        }
        catch(Exception e)
        {
            Log.d(TAG, e.toString());
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

    public void onReadingButtonPressed(View v)
    {
        setContentView(R.layout.activity_read);
    }

    public void onSettingsButtonPressed(View v)
    {
        setContentView(R.layout.activity_settings);
    }

    public void onQuizButtonPressed(View v)
    {
        setContentView(R.layout.activity_quiz);
    }
}