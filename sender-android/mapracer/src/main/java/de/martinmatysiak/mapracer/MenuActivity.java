package de.martinmatysiak.mapracer;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;

import java.io.IOException;
import java.util.UUID;

import de.martinmatysiak.mapracer.data.GameScoresMessage;
import de.martinmatysiak.mapracer.data.GameState;
import de.martinmatysiak.mapracer.data.GameStateMessage;
import de.martinmatysiak.mapracer.data.LoginMessage;
import de.martinmatysiak.mapracer.data.LogoutMessage;
import de.martinmatysiak.mapracer.data.Message;
import de.martinmatysiak.mapracer.data.PlayerState;
import de.martinmatysiak.mapracer.data.PlayerStateMessage;
import de.martinmatysiak.mapracer.data.RequestMessage;


public class MenuActivity
        extends ActionBarActivity
        implements
        CastProvider,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        Cast.MessageReceivedCallback,
        ResultCallback<Cast.ApplicationConnectionResult> {

    public static final String TAG = "MenuActivity";

    boolean mMapLaunched = false;
    SharedPreferences mPreferences;
    PlayerState mPlayerState = PlayerState.WAITING;
    GameState mGameState = GameState.INIT;
    GameStateMessage.Race mRace = null;
    LeaderboardFragment mLeaderboard;

    GoogleApiClient mApiClient;
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
            updateUi();
        }
    };
    MediaRouter mMediaRouter;
    MediaRouteSelector mMediaRouteSelector;
    CastDevice mSelectedDevice;
    MediaRouter.Callback mMediaRouterCallback = new MediaRouter.Callback() {
        @Override
        public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo route) {
            mSelectedDevice = CastDevice.getFromBundle(route.getExtras());
            Log.d(TAG, "Got device: " + mSelectedDevice.getDeviceId());
            createApiClient();
            updateUi();
        }

        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo route) {
            Log.d(TAG, "Device unselected");
            mSelectedDevice = null;
            destroyApiClient(true);
            updateUi();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        Log.d(TAG, "onCreate");

        // Generate a device-bound UUID if there isn't one yet
        mPreferences = getSharedPreferences(Constants.PREFERENCES, MODE_PRIVATE);
        if (!mPreferences.contains(Constants.PREF_UUID)) {
            mPreferences.edit()
                    .putString(Constants.PREF_UUID, UUID.randomUUID().toString())
                    .apply();
        }

        // Configure MediaRouter for our application
        mMediaRouter = MediaRouter.getInstance(getApplicationContext());
        mMediaRouteSelector = new MediaRouteSelector.Builder()
                .addControlCategory(CastMediaControlIntent.categoryForCast(Constants.CAST_APP_ID))
                .build();

        // Check if we are already casting somewhere
        if (savedInstanceState != null) {
            mSelectedDevice = savedInstanceState.getParcelable(Constants.INTENT_DEVICE);
        }

        // Testing
        mLeaderboard = new LeaderboardFragment();
    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");

        mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback,
                MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
        createApiClient();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        updateUi();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop:" + (isFinishing() ? "true" : "false"));
        mMediaRouter.removeCallback(mMediaRouterCallback);
        if (isFinishing()) {
            // Make sure to fully terminate the casting session if we're exiting
            deselectCastDevice();
        } else {
            destroyApiClient();
        }

        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(Constants.INTENT_DEVICE, mSelectedDevice);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu, menu);
        MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
        MediaRouteActionProvider mediaRouteActionProvider =
                (MediaRouteActionProvider) MenuItemCompat.getActionProvider(mediaRouteMenuItem);
        mediaRouteActionProvider.setRouteSelector(mMediaRouteSelector);
        return true;
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        try {
            Cast.CastApi.launchApplication(mApiClient, Constants.CAST_APP_ID, false)
                    .setResultCallback(this);
            updateUi();
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch application", e);
            deselectCastDevice();
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.w(TAG, "GoogleApi connection suspended: " + cause);
        deselectCastDevice();
    }

    @Override
    public void addOnApiClientChangeListener(OnApiClientChangeListener listener) {
        // TODO
    }

    @Override
    public void removeOnApiClientChangeListener(OnApiClientChangeListener listener) {
        // TODO
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.w(TAG, "GoogleApi connection failed: " + result.toString());
        deselectCastDevice();
    }

    public void onClick(View v) {
        // Transmit game data to the cast device
        RequestMessage message = new RequestMessage.Builder()
                .withStart(Constants.DEBUG_START_LOCATION)
                .withTarget(Constants.DEBUG_TARGET_TITLE, Constants.DEBUG_TARGET_LOCATION)
                .build();

        Cast.CastApi.sendMessage(mApiClient, Constants.CAST_NAMESPACE, message.toJson());
    }

    @Override
    public void onResult(Cast.ApplicationConnectionResult result) {
        if (result.getStatus().isSuccess()) {
            // Subscribe to message channel
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
            GameStateMessage gsm = (GameStateMessage) message;
            mGameState = gsm.state;
            mRace = gsm.race;
            ((TextView) findViewById(R.id.player_count)).setText(Integer.toString(gsm.players));

            switch (mGameState) {
                case INIT:
                    mMapLaunched = false;
                    if (mLeaderboard.isAdded()) {
                        getFragmentManager().beginTransaction().remove(mLeaderboard).commit();
                    }
                    break;
                case RACE:
                case SCORES:
                    if (!mLeaderboard.isAdded()) {
                        getFragmentManager().beginTransaction().add(android.R.id.content, mLeaderboard).commit();
                    }
                    break;
            }

            if (mGameState == GameState.INIT) {
                // A new race will be starting, re-allow the map activity to be launched
                mMapLaunched = false;
            }
        } else if (message instanceof PlayerStateMessage) {
            mPlayerState = ((PlayerStateMessage) message).state;
        } else if (message instanceof GameScoresMessage) {
            mLeaderboard.setData(((GameScoresMessage) message).scores);
        }

        updateUi();
        if (!mMapLaunched &&
                mRace != null &&
                mPlayerState == PlayerState.ACTIVE &&
                (mGameState == GameState.LOAD || mGameState == GameState.RACE)) {
            Intent intent = new Intent(this, MapActivity.class);
            intent.putExtra(Constants.INTENT_DEVICE, mSelectedDevice);
            intent.putExtra(Constants.INTENT_STATE, mGameState);
            intent.putExtra(Constants.INTENT_RACE, mRace);
            startActivity(intent);
            mMapLaunched = true;
        }
    }

    private void createApiClient() {
        if (mSelectedDevice == null) {
            return;
        }

        Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions
                .builder(mSelectedDevice, mCastClientListener);

        mApiClient = new GoogleApiClient.Builder(MenuActivity.this)
                .addApi(Cast.API, apiOptionsBuilder.build())
                .addConnectionCallbacks(MenuActivity.this)
                .addOnConnectionFailedListener(MenuActivity.this)
                .build();

        mApiClient.connect();
    }

    /**
     * In case of an error situation, we will simply terminate the whole casting session and prompt
     * the user to try again.
     */
    private void deselectCastDevice() {
        mMediaRouter.selectRoute(mMediaRouter.getDefaultRoute());
        mSelectedDevice = null;
        destroyApiClient(true);
        updateUi();
    }

    private void destroyApiClient() {
        destroyApiClient(false);
    }

    private void destroyApiClient(boolean forceLogout) {
        if (mApiClient != null && mApiClient.isConnected()) {
            // Indicate that the player won't be coming back anytime soon if we are closing the app
            if (forceLogout || isFinishing()) {
                Cast.CastApi.sendMessage(mApiClient, Constants.CAST_NAMESPACE,
                        new LogoutMessage().toJson());
                Cast.CastApi.leaveApplication(mApiClient);
            }

            // Always disconnect the API client onStop as per Guidelines, otherwise we might end up
            // with "ghost" clients still connected to the cast receiver.
            mApiClient.disconnect();
            mApiClient.unregisterConnectionCallbacks(this);
            mApiClient.unregisterConnectionFailedListener(this);
            mApiClient = null;
        }
    }

    private void updateUi() {
        // We'll use an array that represents our "visibility configuration"
        // 0: Intro Text (connect to...), 1: Buttons, 2: Player Count,
        boolean[] visibilities = {true, false, false};

        if (mSelectedDevice != null) {
            visibilities[0] = false;
            visibilities[1] = mGameState == GameState.INIT;
            visibilities[2] = (mGameState == GameState.INIT || mGameState == GameState.LOAD);
        }

        this.findViewById(R.id.text_cast).setVisibility(visibilities[0] ? View.VISIBLE : View.INVISIBLE);
        this.findViewById(R.id.button_quick).setVisibility(visibilities[1] ? View.VISIBLE : View.INVISIBLE);
        this.findViewById(R.id.label_player_count).setVisibility(visibilities[2] ? View.VISIBLE : View.INVISIBLE);
        this.findViewById(R.id.player_count).setVisibility(visibilities[2] ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public GoogleApiClient getApiClient() {
        return mApiClient;
    }

    @Override
    public CastDevice getSelectedDevice() {
        return mSelectedDevice;
    }
}
