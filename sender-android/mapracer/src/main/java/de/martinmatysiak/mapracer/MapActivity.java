package de.martinmatysiak.mapracer;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;

import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.StreetViewPanoramaFragment;
import com.google.android.gms.maps.model.StreetViewPanoramaLocation;

import java.io.IOException;

import de.martinmatysiak.mapracer.data.GameScoresMessage;
import de.martinmatysiak.mapracer.data.GameState;
import de.martinmatysiak.mapracer.data.GameStateMessage;
import de.martinmatysiak.mapracer.data.LoginMessage;
import de.martinmatysiak.mapracer.data.Message;
import de.martinmatysiak.mapracer.data.PositionMessage;


public class MapActivity
        extends Activity
        implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        Cast.MessageReceivedCallback,
        ResultCallback<Cast.ApplicationConnectionResult>,
        StreetViewPanorama.OnStreetViewPanoramaChangeListener {

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
        mPanorama.setOnStreetViewPanoramaChangeListener(this);
        setState((GameState) intent.getSerializableExtra(Constants.INTENT_STATE));

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
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        mApiClient.disconnect();
        super.onDestroy();
    }

    private void setState(GameState state) {
        switch (state) {
            case RACE:
                ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(Constants.VIBRATE_DURATION);
                // no break on purpose!
            case LOAD:
                if (mPanorama != null) {
                    mPanorama.setUserNavigationEnabled(state == GameState.RACE);
                }
                break;
            case SCORES:
                // show some kind of message popup or something
                Log.d(TAG, "Race is done.");
                break;
            case INIT:
                finish();
                break;
        }
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

            Cast.CastApi.sendMessage(mApiClient, Constants.CAST_NAMESPACE, message.toJson());
        } else {
            Log.w(TAG, "ApplicationConnection is not success: " + result.getStatus());
        }
    }

    @Override
    public void onMessageReceived(CastDevice castDevice, String namespace, String json) {
        Log.d(TAG, "onMessageReceived: " + json);
        Message message = Message.fromJson(json);
        if (message instanceof GameStateMessage) {
            setState(((GameStateMessage) message).state);
        } else if (message instanceof GameScoresMessage) {
            GameScoresMessage gsm = (GameScoresMessage) message;
            Log.d(TAG, "First place changed: " + gsm.scores.get(0).name);
        }
    }

    @Override
    public void onStreetViewPanoramaChange(StreetViewPanoramaLocation location) {
        PositionMessage message = new PositionMessage.Builder()
                .withLocation(location.position)
                .build();

        if (mApiClient.isConnected()) {
            Cast.CastApi.sendMessage(mApiClient, Constants.CAST_NAMESPACE, message.toJson());
        }
    }
}
