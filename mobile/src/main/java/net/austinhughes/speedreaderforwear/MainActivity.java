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

    public void onStory1ButtonPressed(View v)
    {
        String story = "To understand meteors, one must also understand meteoroids and meteorites. " +
                "First, a meteoroid is a particle in the solar system. The meteoroid may be as small as a grain of sand, or, as large as a boulder. " +
                "When the meteor enters the Earth\'s atmosphere, and becomes visible as a shooting star, it is called a meteor. " +
                "If the meteor makes it to the ground, it is called a meteorite. Meteors, " +
                "also called shooting stars, occur in the Earth\'s mesosphere at an altitude of about 40-60 miles. " +
                "Millions of meteors enter the Earth\'s atmosphere every day, though the vast majority are observed at night. " +
                "Their visibility in the night sky is due to air friction which causes the meteor to glow and emit a trail of gasses " +
                "and melted particles that lasts for about a second. Meteor showers are relatively common events that occur when the " +
                "Earth passes through a trail of debris left by a comet. Sometimes Meteoroids make it throughout the atmosphere and hit the ground, " +
                "where they are referred to as meteorites. " +
                "There are over 31,000 documented meteorites to have been found, although only five or " +
                "six new ones are found every year. The largest meteorite ever found was in the African nation of Namibia. " +
                "It weighs over 100 tons and left a huge impact crater in the ground. " +
                "Scientists believe the massive Berringer Carter in Arizona was formed when a 300,000 ton meteorite crashed to the ground over 49,000 years ago. " +
                "On November 30, 1954, the Hodges Meteorite (actually a fragment of a meteorite) crashed through the roof of the residence of Ann Hodges in the town of Sylacauga, Alabama. " +
                "It bounced off a table before striking her in the leg. Although she was badly bruised, she was not seriously injured. " +
                "It was the first recorded instance of a meteorite injuring a human. " +
                "The actual meteorite was donated to the Alabama Museum of Natural History after various legal battles concerning ownership. " +
                "Some scientists believe the impact of a large meteorite from an asteroid or comet in Mexico\'s " +
                "Yucatan Peninsula was responsible for the extinction of dinosaurs some 65 million years ago. " +
                "Such an impact would have had catastrophic global consequences including immediate climate change, " +
                "numerous earthquakes, volcano eruptions, wildfires, and massive supertsuanims, along with the proliferation " +
                "of massive amounts of dust and debris that would block solar energy and lead to a disruption in photosynthesis. " +
                "Most meteorites that reach the Earth are classified as chondrites or achondrites, while a small percentage are " +
                "iron meteorites and stony-iron meteorites. Most meteorites are chondrites. Chondrites contain silicate materials that " +
                "were melted in space, amino acids, and other presolar grains, particles likely formed from stellar explosions. Diamond " +
                "and graphite are among materials found to be present in these grains. Chondrites are thought to be over 4.5 billion years of " +
                "age and to have originated in the asteroid belt, where they never formed larger bodies. Achondrites are less common. " +
                "These type of meteorites seem to be similar to igneous rock. Iron meteorites make up less than five percent of meteorite finds. " +
                "These type of meteorites are thought to come from the core of asteroids that were once molten. " +
                "Finally, stony-iron meteorites constitute less than one percent of all meteorite falls. They are made of iron-nickel metal and different silicates.";

        sendDataToWear("CustomText", story);
    }

    public void onStory2ButtonPressed(View v)
    {
        String story = "Peru's Inca Indians first grew potatoes in the Andes in about 200 B.C. " +
                "Spanish conquistadors brought potatoes to Europe, and colonists brought them to " +
                "America. Potatoes are fourth on the list of the world's food staples – after wheat," +
                " corn and rice. Today, Americans consume about 140 pounds of potatoes per person every " +
                "year while Europeans eat twice as many. One of our favorite ways to eat potatoes is in " +
                "the form of potato chips. While Benjamin Franklin was the US ambassador to France, " +
                "he went to a banquet where potatoes were prepared in 20 different ways. Thomas Jefferson, " +
                "who succeeded Franklin as our French ambassador, brought the recipe for thick-cut, " +
                "French-fried potatoes to America. He served French fries to guests at the White House " +
                "in 1802 and at his home, Monticello. On August 24, 1853, at Moon Lake Lodge in " +
                "Saratoga, New York, a native-American chef named George Crum created the first potato chips. " +
                "He became angry when a diner complained that his French fries were too thick, " +
                "so he sliced the potatoes as thinly as possible making them too thin and crisp to eat with a fork. " +
                "The diner loved them, and potato chips were born. In 1860 Chef Crum opened his own restaurant " +
                "and offered a basket of potato chips on every table. In the 1950s, in Ireland, " +
                "Joe \"Spud\" Murphy and Seamus Burke, produced the world's first seasoned crisps, " +
                "Cheese & Onion and Salt & Vinegar. In the United Kingdom and " +
                "Ireland crisps are what we in the United States call potato chips while chips refer to our " +
                "French fries. Ketchup flavored chips are popular in the Mid-East and Canada. Seaweed is popular in the Asia, " +
                "and Mexicans like chicken flavored chips. Other flavors from around the world include: paprika, pickled onion, " +
                "béarnaise, meat pie, Chili Crab, Salmon Teriyaki, Borscht, Caesar Salad, Roasted Sausage, Firecracker Lobster, Roast Ox, Haggis and Black Pepper, " +
                "Olive, and Spaghetti. About 27 pounds of potato chips are produced from 100 pounds of potatoes. " +
                "Americans consume 2-4 billion pounds of potato chips every year, and spend more than $7 billion a year on them.";

        sendDataToWear("CustomText", story);
    }

    public void onStory3ButtonPressed(View v)
    {

    }

    public void onStory4ButtonPressed(View v)
    {

    }

    public void onStory5ButtonPressed(View v)
    {

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