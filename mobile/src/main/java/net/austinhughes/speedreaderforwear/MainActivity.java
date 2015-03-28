/*
    (C) 2015 - Austin Hughes, Stefan Oswald, Nowele Rechka
    Last Modified: 2015-03-28
 */

package net.austinhughes.speedreaderforwear;

// Imports
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ArrayList;

/*
    Main class for phone side application
 */
public class MainActivity extends Activity
{
    // Private class variables
    private GoogleApiClient mGoogleApiClient;
    private PutDataMapRequest dataMap;

    // tag for Log and filenames
    private static final String TAG = "MainActivity"; // Tag for log
    private final String HEADLINES_FILENAME = "rss_headlines";
    private final String DESCRIPTIONS_FILENAME = "rss_descriptions";
    private final String SETTINGS_FILENAME = "settings";

    // Holds the RSS item descriptions
    private String[] descriptions;

    private String rssFeed;
    private int wpm;

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

        // Load in saved settings if any
        try
        {
            FileInputStream fis = openFileInput(SETTINGS_FILENAME);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));

            wpm = Integer.parseInt(br.readLine());
            rssFeed = br.readLine();
        }
        catch(Exception e)
        {
            wpm = 240;
            rssFeed = "http://www.anandtech.com/rss/";
            Log.d(TAG, e.toString());
        }


        // Load in the RSS data
        try
        {
            URL url = new URL(rssFeed);
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

    public void onRSSButtonPressed(View v)
    {
        // Lists to hold headlines and descriptions
        final ArrayList<String> list = new ArrayList<String>();
        final ArrayList<String> list2 = new ArrayList<String>();
        descriptions = new String[0];

        try
        {
            // Create the file readers
            FileInputStream fis = openFileInput(HEADLINES_FILENAME);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            FileInputStream fis2 = openFileInput(DESCRIPTIONS_FILENAME);
            BufferedReader br2 = new BufferedReader(new InputStreamReader(fis2));

            // Load in the first line
            String text = br.readLine();
            String text2 = br2.readLine();
;
            while (text != null && text2 != null)
            {
                // Add them to the list
                list.add(text);
                list2.add(text2);

                // Load in the next line
                text = br.readLine();
                text2 = br2.readLine();
            }

            // Close the file reader
            br.close();
            br2.close();

            // make the descriptions list into an array
            descriptions = list2.toArray(new String[list2.size()]);
        }
        catch(Exception e)
        {
            Log.d(TAG, e.toString());
        }

        // Load the list view with data
        setContentView(R.layout.activity_rss);
        final ListView listview = (ListView) findViewById(R.id.rssList);
        final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, list);
        listview.setAdapter(adapter);

        // Set up item click listener so that when the user clicks an item it sends it to wear to read.
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                Log.d(TAG, "onItemClick Item " + position + " description " + descriptions[position]);
                sendDataToWear("CustomText", descriptions[position]);
                Toast.makeText(getBaseContext(), "Article sent to wear successfully.",
                        Toast.LENGTH_LONG).show();
            }
        });

    }

    // Gets called whenever the send button is pressed
    public void onEditTextPressed(View v)
    {
        // Grab the text from the EditText text field
        EditText mEdit = (EditText)findViewById(R.id.editText);
        mEdit.setText(""); // empty the text field
    }

    // Gets called whenever the send button is pressed
    public void onSendButtonPressed(View v)
    {
        // Grab the text from the EditText text field
        EditText mEdit = (EditText)findViewById(R.id.editText);

        // send the current text to Wear
        sendDataToWear("CustomText", mEdit.getText().toString());

        Toast.makeText(getBaseContext(), "Article sent to wear successfully.",
                Toast.LENGTH_LONG).show();
    }

    // Gets called when the reading list button is pressed, opens the reading list view
    public void onReadingButtonPressed(View v)
    {
        setContentView(R.layout.activity_read);
    }

    // Gets called when the settings button is pressed, opens the settings view
    public void onSettingsButtonPressed(View v)
    {
        setContentView(R.layout.activity_settings);

        EditText wpmText = (EditText)findViewById(R.id.enterReadingSpeed);
        wpmText.setText(Integer.toString(wpm));
        EditText rssFeedText = (EditText)findViewById(R.id.enterRssFeed);
        rssFeedText.setText(rssFeed);
    }

    // Gets called when the quiz button is pressed, opens the quiz view
    public void onQuizButtonPressed(View v)
    {
        setContentView(R.layout.activity_quiz);
    }

    // Gets called when the back button is pressed, opens the main view
    public void onBackButtonPressed(View v) {
        setContentView(R.layout.activity_main);
    }

    public void onSaveSettingsPressed(View v)
    {
        try
        {
            // get new values
            EditText wpmText = (EditText)findViewById(R.id.enterReadingSpeed);
            wpm = (int)Double.parseDouble(wpmText.getText().toString());
            EditText rssFeedText = (EditText)findViewById(R.id.enterRssFeed);
            rssFeed = rssFeedText.getText().toString();

            Log.d(TAG, "New WPM: " + wpm);
            int interval = (int)((60.0/wpm)*1000);
            Log.d(TAG, "Interval will be: " + Integer.toString(interval));
            Log.d(TAG, "new RSS: " + rssFeed);

            // delete old file
            deleteFile(SETTINGS_FILENAME);

            // output new file
            FileOutputStream fos = openFileOutput(SETTINGS_FILENAME, Context.MODE_PRIVATE);
            BufferedWriter SettingsOut = new BufferedWriter(new OutputStreamWriter(fos));

            SettingsOut.write(wpm);
            SettingsOut.newLine();
            SettingsOut.write(rssFeed);
            SettingsOut.close();

            URL url = new URL(rssFeed);
            new RSSReader(getApplicationContext()).execute(url);

            sendDataToWear("Interval", Integer.toString(interval));

            Toast.makeText(getBaseContext(), "Settings Updated!",
                    Toast.LENGTH_LONG).show();
        }
        catch(Exception e)
        {
            Toast.makeText(getBaseContext(), "Error saving settings: " + e.toString(),
                    Toast.LENGTH_LONG).show();
            Log.d(TAG, e.toString());
        }
    }

    // Sends the text to wear
    private void sendDataToWear(String mapLocation, String data)
    {
        // Clear the data map then put the text into it, the Google API client will auto sync it to Wear
        dataMap.getDataMap().clear();
        dataMap.getDataMap().putString(mapLocation, data);
        PutDataRequest request = dataMap.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi
                .putDataItem(mGoogleApiClient, request);
    }
}