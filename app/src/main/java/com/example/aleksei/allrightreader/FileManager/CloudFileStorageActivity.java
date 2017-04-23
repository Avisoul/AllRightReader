package com.example.aleksei.allrightreader.FileManager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.aleksei.allrightreader.R;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi.MetadataBufferResult;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.google.android.gms.drive.widget.DataBufferAdapter;

/**
 * An abstract activity that handles authorization and connection to the Drive
 * services.
 */
public class CloudFileStorageActivity extends Activity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    protected static final int REQUEST_CODE_RESOLUTION = 1;

    private GoogleApiClient mGoogleApiClient;

    private ListView mResultsListView;
    private ResultsAdapter mResultsAdapter;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_cloudfiles);
        mResultsListView = (ListView) findViewById(R.id.listViewResults);
        mResultsAdapter = new ResultsAdapter(this);
        mResultsListView.setAdapter(mResultsAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("Hello", "onResume");

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addScope(Drive.SCOPE_APPFOLDER)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        mGoogleApiClient.connect();
        Log.i("Hello", "onResume 2 ");

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i("Hello", requestCode + " " + resultCode);

        if (requestCode == REQUEST_CODE_RESOLUTION && resultCode == RESULT_OK) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onPause() {
        Log.i("Hello", "onPause");

        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.i("Hello", "onStop");

        super.onStop();
        mResultsAdapter.clear();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i("Hello", "onConnected");

        Query query = new Query.Builder()
                .addFilter(Filters.eq(SearchableField.MIME_TYPE, "application/epub+zip"))
                .build();

        Drive.DriveApi.query(getGoogleApiClient(), query)
                .setResultCallback(metadataCallback);
    }

    final private ResultCallback<MetadataBufferResult> metadataCallback = new
            ResultCallback<MetadataBufferResult>() {
                @Override
                public void onResult(MetadataBufferResult result) {
        if (!result.getStatus().isSuccess()) {
            showMessage("Problem while retrieving results");
            return;
        }
        mResultsAdapter.clear();
        Log.i("Hello", "metadataCallback" + result.getMetadataBuffer().getCount());

        mResultsAdapter.append(result.getMetadataBuffer());
        }
    };

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i("Hello", "GoogleApiClient connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {

        if (!result.hasResolution()) {
            GoogleApiAvailability.getInstance().getErrorDialog(this, result.getErrorCode(), 0).show();
            Log.i("Hello", "GoogleApiClient connection onConnectionFailed 1 " + result.getErrorMessage());

            return;
        }
        try {
            result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
            Log.i("Hello", "GoogleApiClient connection onConnectionFailed " + result.getErrorMessage());

        } catch (SendIntentException e) {
            Log.i("Hello", "GoogleApiClient connection onConnectionFailed 3");
        }
    }

    public void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    public GoogleApiClient getGoogleApiClient() {
        return mGoogleApiClient;
    }
}