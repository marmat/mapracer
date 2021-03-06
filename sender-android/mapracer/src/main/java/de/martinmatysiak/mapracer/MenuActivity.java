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
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;

import java.util.UUID;

import de.martinmatysiak.mapracer.data.GameState;
import de.martinmatysiak.mapracer.data.GameStateMessage;
import de.martinmatysiak.mapracer.data.Message;
import de.martinmatysiak.mapracer.data.PlayerState;
import de.martinmatysiak.mapracer.data.PlayerStateMessage;
import de.martinmatysiak.mapracer.data.RequestMessage;


public class MenuActivity
        extends ActionBarActivity
        implements CastProvider, Cast.MessageReceivedCallback, ConnectionStatusChangeCallback {

    public static final String TAG = MenuActivity.class.getSimpleName();

    ApiClientManager mApiClientManager = new ApiClientManager();

    boolean mMapLaunched = false;
    SharedPreferences mPreferences;
    PlayerState mPlayerState = PlayerState.WAITING;
    GameState mGameState = GameState.INIT;
    GameStateMessage.Race mRace = null;
    LeaderboardFragment mLeaderboard;

    MediaRouter mMediaRouter;
    MediaRouteSelector mMediaRouteSelector;
    MediaRouter.Callback mMediaRouterCallback = new MediaRouter.Callback() {
        @Override
        public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo route) {
            Log.d(TAG, "Device selected");
            mApiClientManager.setSelectedDevice(CastDevice.getFromBundle(route.getExtras()));
        }

        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo route) {
            Log.d(TAG, "Device unselected");
            mApiClientManager.setSelectedDevice(null);
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

        // Initialize our API client manager
        mApiClientManager.init(this);
        mApiClientManager.addConnectionStatusChangeCallback(this);
        mApiClientManager.addMessageReceivedCallback(Constants.CAST_NAMESPACE, this);

        // Check if we are already casting somewhere
        if (savedInstanceState != null) {
            mApiClientManager.setSelectedDevice((CastDevice) savedInstanceState.getParcelable(Constants.INTENT_DEVICE));
        }

        // Testing
        mLeaderboard = new LeaderboardFragment();
    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");

        mApiClientManager.setAutoConnect(true);
        mApiClientManager.connect();
        mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback,
                MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
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
        mApiClientManager.setAutoConnect(false);
        mApiClientManager.disconnect(isFinishing());

        // Make sure to fully terminate the casting session if we're exiting
        if (isFinishing()) {
            deselectCastDevice();
        }

        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(Constants.INTENT_DEVICE, mApiClientManager.getSelectedDevice());
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

    public void onClick(View v) {
        // Transmit game data to the cast device
        RequestMessage message = new RequestMessage.Builder()
                .withStart(Constants.DEBUG_START_LOCATION)
                .withTarget(Constants.DEBUG_TARGET_TITLE, Constants.DEBUG_TARGET_LOCATION)
                .build();

        sendMessage(Constants.CAST_NAMESPACE, message);
    }

    @Override
    public void onMessageReceived(CastDevice castDevice, String namespace, String json) {
        Log.d(TAG, "onMessageReceived: " + json);
        Message message = Message.fromJson(json);

        switch (message.type) {
            case GAME_STATE:
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
                break;
            case PLAYER_STATE:
                mPlayerState = ((PlayerStateMessage) message).state;
                break;
        }

        updateUi();
        if (!mMapLaunched &&
                mRace != null &&
                mPlayerState == PlayerState.ACTIVE &&
                (mGameState == GameState.LOAD || mGameState == GameState.RACE)) {
            Intent intent = new Intent(this, MapActivity.class);
            intent.putExtra(Constants.INTENT_DEVICE, mApiClientManager.getSelectedDevice());
            intent.putExtra(Constants.INTENT_STATE, mGameState);
            intent.putExtra(Constants.INTENT_RACE, mRace);
            startActivity(intent);
            mMapLaunched = true;
        }
    }

    /**
     * In case of an error situation, we will simply terminate the whole casting session and prompt
     * the user to try again.
     */
    private void deselectCastDevice() {
        mMediaRouter.selectRoute(mMediaRouter.getDefaultRoute());
        mApiClientManager.setSelectedDevice(null);
    }

    private void updateUi() {
        // We'll use an array that represents our "visibility configuration"
        // 0: Intro Text (connect to...), 1: Buttons, 2: Player Count,
        boolean[] visibilities = {true, false, false};

        if (mApiClientManager.getConnectionStatus() == ConnectionStatus.CASTING) {
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
    public CastDevice getSelectedDevice() {
        return mApiClientManager.getSelectedDevice();
    }

    @Override
    public ConnectionStatus getConnectionStatus() {
        return mApiClientManager.getConnectionStatus();
    }

    @Override
    public void addConnectionStatusChangeCallback(ConnectionStatusChangeCallback callback) {
        mApiClientManager.addConnectionStatusChangeCallback(callback);
    }

    @Override
    public void removeConnectionStatusChangeCallback(ConnectionStatusChangeCallback callback) {
        mApiClientManager.removeConnectionStatusChangeCallback(callback);
    }

    @Override
    public void addMessageReceivedCallback(String namespace, Cast.MessageReceivedCallback callback) {
        mApiClientManager.addMessageReceivedCallback(namespace, callback);
    }

    @Override
    public void removeMessageReceivedCallback(String namespace, Cast.MessageReceivedCallback callback) {
        mApiClientManager.removeMessageReceivedCallback(namespace, callback);
    }

    @Override
    public PendingResult<Status> sendMessage(String namespace, Message message) {
        return mApiClientManager.sendMessage(namespace, message);
    }

    @Override
    public void onConnectionStatusChange(ConnectionStatus connectionStatus) {
        updateUi();
    }
}
