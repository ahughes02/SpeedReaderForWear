/*
    (C) 2015 - Austin Hughes, Stefan Oswald, Nowele Rechka
    Last Modified: 2015-02-12
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
    private static final String TAG = "RSS Reader"; // Tag for log
    private final String HEADLINES_FILENAME = "rss_headlines";
    private final String DESCRIPTIONS_FILENAME = "rss_descriptions";

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
        String text = null;
        int event;

        try
        {
            // Make sure file is clean
            ctx.deleteFile(HEADLINES_FILENAME);
            ctx.deleteFile(DESCRIPTIONS_FILENAME);

            Log.d(TAG, "Start Download");

            // Write to the file
            FileOutputStream fos = ctx.openFileOutput(HEADLINES_FILENAME, Context.MODE_PRIVATE);
            BufferedWriter HeadlinesOut = new BufferedWriter(new OutputStreamWriter(fos));

            FileOutputStream fos2 = ctx.openFileOutput(DESCRIPTIONS_FILENAME, Context.MODE_PRIVATE);
            BufferedWriter DescriptionsOut = new BufferedWriter(new OutputStreamWriter(fos2));

            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(false);
            XmlPullParser xpp = factory.newPullParser();

            // We will get the XML from an input stream
            xpp.setInput(getInputStream(url), "UTF-8");
            event = xpp.getEventType();
            while (event != XmlPullParser.END_DOCUMENT)
            {
                String name = xpp.getName();

                switch (event)
                {
                    case XmlPullParser.START_TAG:
                        break;
                    case XmlPullParser.TEXT:
                        text = xpp.getText();
                        break;
                    case XmlPullParser.END_TAG:
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
                                text = CleanDescription(text);
                                Log.d(TAG, "Description: " + text);
                                DescriptionsOut.write(text);
                                DescriptionsOut.newLine();
                        }
                        break;
                }
                event = xpp.next();
            }

            DescriptionsOut.close();
            HeadlinesOut.close();
        }
        catch (Exception e)
        {
            Log.d(TAG, e.toString());
        }

        return true;
    }

    private String CleanDescription(String description)
    {
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
        description = description.replace("\n", "").replace("\r", "");
        return android.text.Html.fromHtml(description).toString();
    }

    private InputStream getInputStream(URL url) {
        try {
            return url.openConnection().getInputStream();
        } catch (IOException e) {
            return null;
        }
    }
}
