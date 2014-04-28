package de.martinmatysiak.mapracer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;


public class MapActivity
        extends Activity
        implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        Cast.MessageReceivedCallback,
        ResultCallback<Cast.ApplicationConnectionResult> {

    final static String TAG = "MapActivity";

    CastDevice mSelectedDevice;
    GoogleApiClient mApiClient;
    LatLng mStartLocation;
    LatLng mTargetLocation;
    String mTargetTitle;


    Cast.Listener mCastClientListener = new Cast.Listener() {
        @Override
        public void onApplicationStatusChanged() {
            if (mApiClient != null) {
                Log.d(TAG, "onApplicationStatusChanged: "
                        + Cast.CastApi.getApplicationStatus(mApiClient));
            }
        }

        @Override
        public void onVolumeChanged() {
            if (mApiClient != null) {
                Log.d(TAG, "onVolumeChanged: " + Cast.CastApi.getVolume(mApiClient));
            }
        }

        @Override
        public void onApplicationDisconnected(int errorCode) {
            Log.w(TAG, "onApplicationDisconnected: " + errorCode);
            mSelectedDevice = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Gather game-specific parameters
        Intent intent = getIntent();
        mStartLocation = intent.getParcelableExtra(Constants.DATA_START_LOCATION);
        mTargetLocation = intent.getParcelableExtra(Constants.DATA_TARGET_LOCATION);
        mTargetTitle = intent.getStringExtra(Constants.DATA_TARGET_TITLE);
        mSelectedDevice = intent.getParcelableExtra(Constants.INTENT_DEVICE);
        Log.d(TAG, "Device: " + mSelectedDevice.getDeviceId());

        // Initialize the StreetView
        WebView streetView = (WebView) findViewById(R.id.streetView);

        streetView.getSettings().setJavaScriptEnabled(true);
        streetView.addJavascriptInterface(this, "AndroidCast");
        streetView.loadUrl("file:///android_asset/index.html#" +
                mStartLocation.latitude + "," + mStartLocation.longitude);

        // Reconnect to the cast device and enable communication
        Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions
                .builder(mSelectedDevice, mCastClientListener);

        mApiClient = new GoogleApiClient.Builder(MapActivity.this)
                .addApi(Cast.API, apiOptionsBuilder.build())
                .addConnectionCallbacks(MapActivity.this)
                .addOnConnectionFailedListener(MapActivity.this)
                .build();

        mApiClient.connect();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        try {
            Cast.CastApi.launchApplication(mApiClient, Constants.CAST_APP_ID, false)
                    .setResultCallback(this);
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch application", e);
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.w(TAG, "GoogleApi connection suspended: " + cause);
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.w(TAG, "GoogleApi connection failed: " + result.toString());
    }

    @Override
    public void onResult(Cast.ApplicationConnectionResult result) {
        if (result.getStatus().isSuccess()) {
            try {
                Cast.CastApi.setMessageReceivedCallbacks(mApiClient, Constants.CAST_NAMESPACE, this);
            } catch (IOException e) {
                Log.e(TAG, "Exception while creating channel", e);
            }
        } else {
            Log.w(TAG, "ApplicationConnection is not success: " + result.getStatus());
        }
    }

    @Override
    public void onMessageReceived(CastDevice castDevice, String namespace, String message) {
        Log.d(TAG, "onMessageReceived: " + message);
    }

    @JavascriptInterface
    public void sendMessage(String message) {
        Cast.CastApi.sendMessage(mApiClient, Constants.CAST_NAMESPACE, message);
    }
}
