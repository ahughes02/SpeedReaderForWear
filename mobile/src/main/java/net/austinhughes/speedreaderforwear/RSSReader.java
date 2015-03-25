/*
    (C) 2015 - Austin Hughes, Stefan Oswald, Nowele Rechka
    Last Modified: 2015-02-12
 */

package net.austinhughes.speedreaderforwear;

import android.os.AsyncTask;
import android.util.Log;
import android.content.Context;
import android.os.Bundle;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URL;

public class RSSReader extends AsyncTask<URL, Void, Boolean>
{
    private static final String TAG = "RSS Reader"; // Tag for log
    private final String HEADLINES_FILENAME = "rss_headlines";

    private Context ctx;

    public RSSReader (Context context)
    {
        ctx = context;
    }

    public Boolean doInBackground(URL... urls)
    {
        return LoadRSSHeadlines(urls[0]);
    }

    public void onPostExecute(Boolean result)
    {
        Log.d(TAG, "Finished download");
    }

    public Boolean LoadRSSHeadlines(URL url)
    {
        try
        {
            // Make sure file is clean
            ctx.deleteFile(HEADLINES_FILENAME);

            // Write to the file
            FileOutputStream fos = ctx.openFileOutput(HEADLINES_FILENAME, Context.MODE_PRIVATE);
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(fos));

            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(false);
            XmlPullParser xpp = factory.newPullParser();

            // We will get the XML from an input stream
            xpp.setInput(getInputStream(url), "UTF_8");

        /* We will parse the XML content looking for the "<title>" tag which appears inside the "<item>" tag.
         * However, we should take in consideration that the rss feed name also is enclosed in a "<title>" tag.
         * As we know, every feed begins with these lines: "<channel><title>Feed_Name</title>...."
         * so we should skip the "<title>" tag which is a child of "<channel>" tag,
         * and take in consideration only "<title>" tag which is a child of "<item>"
         *
         * In order to achieve this, we will make use of a boolean variable.
         */
            boolean insideItem = false;

            // Returns the type of current event: START_TAG, END_TAG, etc..
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT)
            {
                if (eventType == XmlPullParser.START_TAG)
                {
                    if (xpp.getName().equalsIgnoreCase("item"))
                    {
                        insideItem = true;
                    }
                    else if (xpp.getName().equalsIgnoreCase("title"))
                    {
                        if (insideItem)
                        {
                            String text = xpp.nextText();
                            Log.d(TAG, "Extracted link: " + text);
                            out.write(text);
                            out.newLine();
                        }
                    }
                    else if (xpp.getName().equalsIgnoreCase("link"))
                    {
                        if (insideItem)
                        {
                            String text = xpp.nextText();
                            Log.d(TAG, "Extracted link: " + text);
                            out.write(text);
                            out.newLine();
                        }
                    }
                }
                else if(eventType==XmlPullParser.END_TAG && xpp.getName().equalsIgnoreCase("item"))
                {
                    insideItem = false;
                }

                eventType = xpp.next(); //move to next element
            }

            out.close();

        }
        catch (Exception e)
        {
            Log.d(TAG, "Exception: " + e.toString());
        }

        return true;
    }

    private InputStream getInputStream(URL url) {
        try {
            return url.openConnection().getInputStream();
        } catch (IOException e) {
            return null;
        }
    }
}
