package de.martinmatysiak.mapracer;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.StreetViewPanoramaFragment;

import java.io.IOException;

import de.martinmatysiak.mapracer.data.GameStateMessage;
import de.martinmatysiak.mapracer.data.LoginMessage;
import de.martinmatysiak.mapracer.data.Message;


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
    GameStateMessage.Race mRace;
    SharedPreferences mPreferences;
    StreetViewPanorama mPanorama;


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
        mPreferences = getSharedPreferences(Constants.PREFERENCES, MODE_PRIVATE);

        // Gather game-specific parameters
        Intent intent = getIntent();
        mRace = intent.getParcelableExtra(Constants.INTENT_RACE);
        mSelectedDevice = intent.getParcelableExtra(Constants.INTENT_DEVICE);
        Log.d(TAG, "Device: " + mSelectedDevice.getDeviceId());

        // Initialize the StreetViewPanorama
        mPanorama = ((StreetViewPanoramaFragment) getFragmentManager().findFragmentById(R.id.streetView)).getStreetViewPanorama();
        mPanorama.setPosition(mRace.startLocation);

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

            // Login with our UUID
            LoginMessage message = new LoginMessage.Builder()
                    .withId(mPreferences.getString(Constants.PREF_UUID, ""))
                    .build();

            Cast.CastApi.sendMessage(mApiClient, Constants.CAST_NAMESPACE,
                    Message.getConfiguredGson().toJson(message));
        } else {
            Log.w(TAG, "ApplicationConnection is not success: " + result.getStatus());
        }
    }

    @Override
    public void onMessageReceived(CastDevice castDevice, String namespace, String message) {
        Log.d(TAG, "onMessageReceived: " + message);
    }
}
