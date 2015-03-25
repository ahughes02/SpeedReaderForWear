/*
    (C) 2015 - Austin Hughes, Stefan Oswald, Nowele Rechka
    Last Modified: 2015-01-01
 */

package net.austinhughes.speedreaderforwear;

// Imports
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.*;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.*;


public class ReadingListActivity
{

    // Gets called whenever the send button is pressed
    public void onSendButtonPressed(View v)
    {
        EditText mEdit = (EditText)findViewById(R.id.editText);

        dataMap.getDataMap().putString("editTextValue", mEdit.getText().toString());
        PutDataRequest request = dataMap.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi
                .putDataItem(mGoogleApiClient, request);

        Toast.makeText(getBaseContext(), "Sent text to Wear device",
                Toast.LENGTH_LONG).show();
    }
}