/*
    (C) 2015 - Austin Hughes, Stefan Oswald, Nowele Rechka
    Last Modified: 2015-03-28
 */

package net.austinhughes.speedreaderforwear;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URL;

public class RSSReader extends AsyncTask<URL, Void, Boolean>
{
    // Tag for Log and filenames
    private static final String TAG = "RSS Reader"; // Tag for log
    private final String HEADLINES_FILENAME = "rss_headlines";
    private final String DESCRIPTIONS_FILENAME = "rss_descriptions";

    // Stores the current app context
    private Context ctx;

    // Constructor
    public RSSReader (Context context)
    {
        ctx = context;
    }

    // Entry point for AsyncTask
    public Boolean doInBackground(URL... urls)
    {
        return LoadRSSHeadlines(urls[0]);
    }

    // Called when doInBackground finishes
    public void onPostExecute(Boolean result)
    {
        Log.d(TAG, "Finished download");
    }

    // Loads in the RSS data for the given url
    public Boolean LoadRSSHeadlines(URL url)
    {
        // Variables for RSS parsing
        String text = null;
        int event;

        try
        {
            // Make sure files are clean
            ctx.deleteFile(HEADLINES_FILENAME);
            ctx.deleteFile(DESCRIPTIONS_FILENAME);

            // Open file writers
            FileOutputStream fos = ctx.openFileOutput(HEADLINES_FILENAME, Context.MODE_PRIVATE);
            BufferedWriter HeadlinesOut = new BufferedWriter(new OutputStreamWriter(fos));
            FileOutputStream fos2 = ctx.openFileOutput(DESCRIPTIONS_FILENAME, Context.MODE_PRIVATE);
            BufferedWriter DescriptionsOut = new BufferedWriter(new OutputStreamWriter(fos2));

            // Get XML parser
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(false);
            XmlPullParser xpp = factory.newPullParser();

            // Get input stream from URL
            xpp.setInput(getInputStream(url), "UTF-8");
            event = xpp.getEventType();

            while (event != XmlPullParser.END_DOCUMENT)
            {
                String name = xpp.getName();
                // Switch based on event
                switch (event)
                {
                    case XmlPullParser.START_TAG:
                        break;
                    case XmlPullParser.TEXT:
                        text = xpp.getText();
                        break;
                    case XmlPullParser.END_TAG:
                        // Write title to file
                        if(name.equals("title"))
                        {
                            Log.d(TAG, "Title: " + text);
                            HeadlinesOut.write(text);
                            HeadlinesOut.newLine();
                        }
                        else if(name.equals("link"))
                        {
                            Log.d(TAG, "Link: " + text);
                        }
                        else if(name.equals("description"))
                        {
                            // Write description to file
                            text = CleanDescription(text);
                            Log.d(TAG, "Description: " + text);
                            DescriptionsOut.write(text);
                            DescriptionsOut.newLine();
                        }
                        break;
                }
                event = xpp.next();
            }

            // close out file writers
            DescriptionsOut.close();
            HeadlinesOut.close();
        }
        catch (Exception e)
        {
            Log.d(TAG, e.toString());
        }

        return true;
    }

    // Makes sure no html tags or new lines are in the string
    private String CleanDescription(String description)
    {
        // Remove HTML tags
        int index=0;
        int index2=0;
        while(index!=-1)
        {
            index = description.indexOf("<");
            index2 = description.indexOf(">", index);
            if(index!=-1 && index2!=-1)
            {
                description = description.substring(0, index).concat(description.substring(index2+1, description.length()));
            }
        }
        // Remove new lines
        description = description.replace("\n", "").replace("\r", "");
        // Make sure no other HTML elements exist
        return android.text.Html.fromHtml(description).toString();
    }

    // Gets input stream from a URL
    private InputStream getInputStream(URL url)
    {
        try
        {
            return url.openConnection().getInputStream();
        }
        catch (IOException e) {
            return null;
        }
    }
}
