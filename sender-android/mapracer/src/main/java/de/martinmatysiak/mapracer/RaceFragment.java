package de.martinmatysiak.mapracer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

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
import de.martinmatysiak.mapracer.data.PlayerState;
import de.martinmatysiak.mapracer.data.PlayerStateMessage;
import de.martinmatysiak.mapracer.data.PositionMessage;


/**
 * The RaceFragment handles all logic needed during an active race, i.e. passing along the user
 * input to the Cast Receiver and processing State updates.
 */
public class RaceFragment extends StreetViewPanoramaFragment implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        Cast.MessageReceivedCallback,
        ResultCallback<Cast.ApplicationConnectionResult>,
        StreetViewPanorama.OnStreetViewPanoramaChangeListener {

    final static String TAG = RaceFragment.class.getSimpleName();

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

    CastDevice mSelectedDevice;
    GoogleApiClient mApiClient;
    GameStateMessage.Race mRace;
    SharedPreferences mPreferences;
    StreetViewPanorama mPanorama;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param race   The race that this fragment will be used in.
     * @param state  The current game state.
     * @param device The receiver with which we are connected.
     * @return A new instance of fragment RaceFragment.
     */
    public static RaceFragment newInstance(GameStateMessage.Race race, GameState state, CastDevice device) {
        RaceFragment fragment = new RaceFragment();
        Bundle args = new Bundle();
        args.putParcelable(Constants.INTENT_RACE, race);
        args.putParcelable(Constants.INTENT_DEVICE, device);
        args.putSerializable(Constants.INTENT_STATE, state);
        fragment.setArguments(args);
        return fragment;
    }

    public RaceFragment() { /* Required empty public constructor */ }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPreferences = getActivity().getSharedPreferences(Constants.PREFERENCES, Context.MODE_PRIVATE);
        if (getArguments() != null) {
            setRace((GameStateMessage.Race) getArguments().getParcelable(Constants.INTENT_RACE));
            setState((GameState) getArguments().getSerializable(Constants.INTENT_STATE));
            setDevice((CastDevice) getArguments().getParcelable(Constants.INTENT_DEVICE));
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        mApiClient.disconnect();
        super.onDestroy();
    }

    public void setRace(GameStateMessage.Race race) {
        mRace = race;

        // (Re)initialize the StreetViewPanorama
        mPanorama = getStreetViewPanorama();
        mPanorama.setPosition(mRace.startLocation);
        mPanorama.setOnStreetViewPanoramaChangeListener(this);
    }

    public void setState(GameState state) {
        switch (state) {
            case RACE:
                ((Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(Constants.VIBRATE_DURATION);
                // no break on purpose!
            case LOAD:
                if (mPanorama != null) {
                    mPanorama.setUserNavigationEnabled(state == GameState.RACE);
                }
                break;
            case SCORES:
            case INIT:
                // ??? indicate that the fragment can be closed ???
                break;
        }
    }

    public void setDevice(CastDevice device) {
        mSelectedDevice = device;

        // Reconnect to the cast device and enable communication
        Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions
                .builder(mSelectedDevice, mCastClientListener);

        mApiClient = new GoogleApiClient.Builder(getActivity())
                .addApi(Cast.API, apiOptionsBuilder.build())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
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
        } else if (message instanceof PlayerStateMessage) {
            if (((PlayerStateMessage) message).state == PlayerState.FINISHED) {
                Toast.makeText(getActivity(), "You've finished the race!", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onStreetViewPanoramaChange(StreetViewPanoramaLocation location) {
        if (mApiClient == null || !mApiClient.isConnected()) {
            return;
        }

        PositionMessage message = new PositionMessage.Builder()
                .withLocation(location.position)
                .build();
        Cast.CastApi.sendMessage(mApiClient, Constants.CAST_NAMESPACE, message.toJson());
    }

}
