package de.martinmatysiak.mapracer;


import android.content.Intent;
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

import de.martinmatysiak.mapracer.data.GameState;
import de.martinmatysiak.mapracer.data.GameStateMessage;
import de.martinmatysiak.mapracer.data.Message;
import de.martinmatysiak.mapracer.data.PlayerState;
import de.martinmatysiak.mapracer.data.PlayerStateMessage;
import de.martinmatysiak.mapracer.data.RequestMessage;


public class MenuActivity
        extends ActionBarActivity
        implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        Cast.MessageReceivedCallback,
        ResultCallback<Cast.ApplicationConnectionResult> {

    public static final String TAG = "MenuActivity";

    String mPlayerState = PlayerState.WAITING;
    GameState mGameState = GameState.INIT;
    GameStateMessage.Race mRace = null;

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
        public void onVolumeChanged() {
            if (mApiClient != null) {
                Log.d(TAG, "onVolumeChanged: " + Cast.CastApi.getVolume(mApiClient));
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

            Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions
                    .builder(mSelectedDevice, mCastClientListener);

            mApiClient = new GoogleApiClient.Builder(MenuActivity.this)
                    .addApi(Cast.API, apiOptionsBuilder.build())
                    .addConnectionCallbacks(MenuActivity.this)
                    .addOnConnectionFailedListener(MenuActivity.this)
                    .build();

            mApiClient.connect();
        }

        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo route) {
            Log.d(TAG, "Device unselected");
            mSelectedDevice = null;
            updateUi();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        mMediaRouter = MediaRouter.getInstance(getApplicationContext());
        mMediaRouteSelector = new MediaRouteSelector.Builder()
                .addControlCategory(CastMediaControlIntent.categoryForCast(Constants.CAST_APP_ID))
                .build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback,
                MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
    }

    @Override
    protected void onPause() {
        if (isFinishing()) {
            mMediaRouter.removeCallback(mMediaRouterCallback);
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (mSelectedDevice != null) {
            Cast.CastApi.leaveApplication(mApiClient);
        }
        super.onStop();
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
    public boolean onOptionsItemSelected(MenuItem item) {
        return item.getItemId() == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        try {
            Cast.CastApi.launchApplication(mApiClient, Constants.CAST_APP_ID, false)
                    .setResultCallback(this);
            updateUi();
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

    public void onClick(View v) {
        // Transmit game data to the cast device
        RequestMessage message = new RequestMessage.Builder()
                .withStart(Constants.DEBUG_START_LOCATION)
                .withTarget(Constants.DEBUG_TARGET_TITLE, Constants.DEBUG_TARGET_LOCATION)
                .build();

        Cast.CastApi.sendMessage(mApiClient, Constants.CAST_NAMESPACE,
                Message.getConfiguredGson().toJson(message));
    }

    private void updateUi() {
        int buttons = mSelectedDevice != null ? View.VISIBLE : View.INVISIBLE;
        int text = mSelectedDevice != null ? View.INVISIBLE : View.VISIBLE;

        //this.findViewById(R.id.button_custom).setVisibility(buttons);
        this.findViewById(R.id.button_quick).setVisibility(buttons);
        this.findViewById(R.id.label_player_count).setVisibility(buttons);
        this.findViewById(R.id.player_count).setVisibility(buttons);
        this.findViewById(R.id.text_cast).setVisibility(text);
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
    public void onMessageReceived(CastDevice castDevice, String namespace, String json) {
        Log.d(TAG, "onMessageReceived: " + json);

        Message message = Message.getConfiguredGson().fromJson(json, Message.class);

        if (message instanceof GameStateMessage) {
            GameStateMessage gsm = (GameStateMessage) message;
            mGameState = gsm.state;
            mRace = gsm.race;
            ((TextView) findViewById(R.id.player_count)).setText(Integer.toString(gsm.players));
            Log.d(TAG, Message.getConfiguredGson().toJson(gsm));
        } else if (message instanceof PlayerStateMessage) {
            mPlayerState = ((PlayerStateMessage) message).state;
        }

        if (mRace != null &&
                mPlayerState.equals(PlayerState.ACTIVE) &&
                (mGameState.equals(GameState.LOAD) || mGameState.equals(GameState.RACE))) {
            Intent intent = new Intent(this, MapActivity.class);
            intent.putExtra(Constants.INTENT_DEVICE, mSelectedDevice);
            intent.putExtra(Constants.INTENT_RACE, mRace);
            startActivity(intent);
        }
    }
}
