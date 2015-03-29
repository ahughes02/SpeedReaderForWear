/*
    (C) 2015 - Austin Hughes, Stefan Oswald, Nowele Rechka
    Last Modified: 2015-03-29
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

    public void onBackButtonPressedFromQuiz(View v)
    {
        setContentView(R.layout.activity_quiz);
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

    public void onCorrectAnswerPressed(View v)
    {
        Toast.makeText(getBaseContext(), "Correct!",
                Toast.LENGTH_LONG).show();
    }

    public void onWrongAnswerPressed(View v)
    {
        Toast.makeText(getBaseContext(), "Sorry, that is the wrong answer.",
                Toast.LENGTH_LONG).show();
    }

    public void onQuiz1ButtonPressed(View v)
    {
        setContentView(R.layout.quiz1_question1);
    }

    public void onQuiz2ButtonPressed(View v)
    {
        setContentView(R.layout.quiz2_question1);
    }

    public void onQuiz3ButtonPressed(View v)
    {
        setContentView(R.layout.quiz3_question1);
    }

    public void onQuiz4ButtonPressed(View v)
    {
        setContentView(R.layout.quiz4_question1);
    }

    public void onQuiz5ButtonPressed(View v)
    {
        setContentView(R.layout.quiz5_question1);
    }

    public void onQuiz1Question1NextPressed(View v)
    {
        setContentView(R.layout.quiz1_question2);
    }
    public void onQuiz1Question2NextPressed(View v)
    {
        setContentView(R.layout.quiz1_question3);
    }
    public void onQuiz1Question3NextPressed(View v)
    {
        setContentView(R.layout.quiz1_question4);
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
        Toast.makeText(getBaseContext(), "Article sent to wear successfully.",
                Toast.LENGTH_LONG).show();
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
        Toast.makeText(getBaseContext(), "Article sent to wear successfully.",
                Toast.LENGTH_LONG).show();
    }

    public void onStory3ButtonPressed(View v)
    {
        String story = "A tornado is born from a powerful storm called a supercell. Tornadoes have " +
                "been reported in all states, but most tornadoes happen in the central parts of America called “Tornado Alley.” " +
                "In some supercells, warm, moist air rises quickly into the atmosphere. Winds blowing at " +
                "different speeds at different parts of the supercell produce wind shear and cause a " +
                "horizontal, rotating column of air. A funnel cloud will form as the air column rotates " +
                "faster and more tightly within the supercell. The rain and hail within the storm cause " +
                "the funnel cloud to touch the ground resulting in a tornado. The strength of a tornado " +
                "is measured by what’s called the Fujita scale. The weakest tornadoes (F0) feature winds " +
                "of 40-78 miles per hour, while the strongest tornadoes (F5) have winds of up to 318 miles" +
                " per hour. All tornadoes can be devastating, especially if they touch down in areas with lots of people. " +
                "Tornado Outbreak" +
                "A tornado outbreak occurs when one storm system produces multiple tornadoes. Some tornado " +
                "outbreaks can result in the formation of dozens of tornadoes over several states. " +
                "One particularly powerful tornado outbreak occurred between April 25 and April 28 of 2011, " +
                "where a record 355 tornadoes in 21 states and Canada were recorded, an including an " +
                "F5 tornado that completely destroyed parts of Tuscaloosa, Alabama. Much of the destruction " +
                "was caught on camera and broadcast across the country and internet. The same weather system " +
                "produced hailstones that measured 4.5 inches across in southern Virginia. 328 people were killed " +
                "as a result of the outbreak, which totaled over $11 billion in damages.";
        sendDataToWear("CustomText", story);
        Toast.makeText(getBaseContext(), "Article sent to wear successfully.",
                Toast.LENGTH_LONG).show();
    }

    public void onStory4ButtonPressed(View v)
    {
        String story = "William Henry Gates III (Bill) was born on October 28, 1955, in Seattle, Washington. " +
                "Bill was the second of three children in an upper-middle class family. He enjoyed playing games " +
                "with the family and was very competitive. He also loved to read. Bill became bored in public " +
                "school so his family sent him to Lakeside School, a private school, where he excelled in math " +
                "and science and did well in drama and English.\n" +
                "Gates became interested in computer programming when he was 13, during the era of " +
                "giant mainframe computers. His school held a fund-raiser to purchase a teletype terminal " +
                "so students could use computer time that was donated by General Electric. " +
                "Using this time, Gates wrote a tic-tac-toe program using BASIC, one of the first computer " +
                "languages. Later he created a computer version of Risk, a board game he liked in which the " +
                "goal is world domination. At Lakeside, Bill met Paul Allen, who shared his interest in computers. " +
                "Gates and Allen and two other students hacked into a computer belonging to Computer Center Corporation (CCC) " +
                "to get free computer time but were caught. After a period of probation, " +
                "they were allowed back in the computer lab when they offered to fix glitches in CCC’s software. " +
                "At age 17, Gates and Allen were paid $20,000 for a program called Traf-O-Data that was used to count traffic.\n" +
                "In early 1973, Bill Gates served as a congressional page in the U.S. House of Representatives. " +
                "He scored 1590 out of 1600 on the SAT and was accepted by Harvard University. " +
                "Steve Ballmer, who became CEO of Microsoft after Bill retired, was also a Harvard student." +
                " Meanwhile, Paul Allen dropped out of Washington College to work on computers at " +
                "Honeywell Corporation and convinced Gates to drop out of Harvard and join him in starting a " +
                "new software company in Albuquerque, New Mexico. They called it Micro-Soft. " +
                "This was soon changed to Microsoft, and they moved their company to Bellevue, Washington.\n" +
                "In 1980, IBM, one of the largest technology companies of the era, asked Microsoft " +
                "to write software to run their new personal computer, the IBM PC. Microsoft kept the " +
                "licensing rights for the operating system (MS-DOS) so that they earned money for every " +
                "computer sold first by IBM, and later by all the other companies that made PC computers. " +
                "Microsoft grew quickly from 25 employees in 1978 to over 90,000 today. Over the years, " +
                "Microsoft developed many new technologies and some of the world’s most popular software " +
                "and products such as Word and Power Point. Although some have criticized Gates for using " +
                "questionable business practices, he built Microsoft into one of the largest companies in " +
                "the world. He has been described as brilliant but childlike, driven, competitive, intense, fun, but lacking in empathy.\n" +
                "Bill Gates is one of the richest men in the world. In 2012, his $61 billion dollars " +
                "in assets made him the world's second richest man according to Forbes Magazine. " +
                "In 2006, Gates announced that he would cut back his involvement at Microsoft to spend" +
                " more time on philanthropy and his foundation. The Bill and Melinda Gates Foundation " +
                "supports many causes including the quest to eradicate Polio, fighting AIDS, malaria and tuberculosis;" +
                " providing vaccinations for children; and even reinventing the toilet among many other things.";
        sendDataToWear("CustomText", story);
        Toast.makeText(getBaseContext(), "Article sent to wear successfully.",
                Toast.LENGTH_LONG).show();
    }

    public void onStory5ButtonPressed(View v)
    {
        String story = "The Age of Exploration started in the 1400's. Europeans were desperate to get spices from Asia. " +
                "Spices were used to preserve foods and keep them from spoiling. Spices, however, were expensive and dangerous to get. " +
                "European rulers began to pay for explorations to find a sea route to Asia so they could get spices cheaper. \n" +
                "Portugal was the first country that sent explorers to search for the sea route to Asia." +
                " After Bartholomew Dias and his crew made it to Africa's Cape of Good Hope, Vasco da Gama " +
                "and his crew became the first to sail around Africa and through the Indian Ocean to India. " +
                "Spain, however, would soon take over the lead in exploration. When Portugal refused to finance " +
                "Christopher Columbus' idea to sail west to find the shortcut to the Indies, Columbus convinced Spain's" +
                " King Ferdinand and Queen Isabella to finance it. On October 12, 1492, Christopher Columbus and his crew " +
                "reached the island of Hispaniola. Although Columbus believed he had reached Asia, he had actually discovered " +
                "the entire continent of North America and claimed it for Spain. \n" +
                "Spain quickly colonized North America. Ponce de Leon discovered Florida, and the first European " +
                "settlement in the New World was later established at St. Augustine. Hernando Cortes crushed the Aztec " +
                "empire in Mexico and claimed it for Spain. Francisco Pizarro did the same to the Incan Empire in South America." +
                " Other explorers such as Francisco Coronado and Hernando de Soto claimed other portions of North America for Spain." +
                " Vasco Nunez de Balboa even claimed the entire Pacific Ocean for Spain. As the Spanish empire grew, explorers " +
                "forced native populations into slavery and to convert to Christianity. Meanwhile, France began to explore North America." +
                " Explorations by Giovanni Verrazano and Jacques Cartier resulted in French claims of much of Canada and the north Atlantic coast." +
                " England would soon attempt to make its presence known by financing pirates such as Francis Drake to plunder Spanish " +
                "settlements and steal gold from Spanish sea vessels. England also established a settlement in North Carolina in 1587." +
                " Territorial disputes and constant pirating resulted in a series of major wars between the competing nations. In 1588," +
                " the British Army defeated the vaunted Spanish Armada. The British victory proved a serious blow to Spanish influence in the New World. \n" +
                "Although Spain still controlled much of the New World after defeat, England and France were able to accelerate their" +
                " colonization. England soon established successful colonies throughout the eastern portions of the United States, " +
                "and France had colonies in Canada and the middle portions of the United States. By the mid 1700's, new territorial " +
                "disputes between England and France eventually resulted in England gaining control over much of North America after" +
                " the French and Indian War. English colonies flourished in North America until 1776 when the colonists declared their independence. " +
                "The Revolutionary War ensued and resulted in independence for the colonists. The United States of America was formed.";
        sendDataToWear("CustomText", story);
        Toast.makeText(getBaseContext(), "Article sent to wear successfully.",
                Toast.LENGTH_LONG).show();
    }

}