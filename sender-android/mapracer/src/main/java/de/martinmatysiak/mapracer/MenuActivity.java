package de.martinmatysiak.mapracer;


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

import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;


public class MenuActivity
        extends ActionBarActivity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static final String TAG = "mapracer";
    public static final String APP_ID = "807AB2E8";

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
            updateUi();
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
                .addControlCategory(CastMediaControlIntent.categoryForCast(APP_ID))
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
        int id = item.getItemId();
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        try {
            // Just launch the application to get the splash screen,
            // we don't care about interaction just yet
            Cast.CastApi.launchApplication(mApiClient, APP_ID, false);
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

    private void updateUi() {
        int buttons = mSelectedDevice != null ? View.VISIBLE : View.INVISIBLE;
        int text = mSelectedDevice != null ? View.INVISIBLE : View.VISIBLE;

        this.findViewById(R.id.button_custom).setVisibility(buttons);
        this.findViewById(R.id.button_quick).setVisibility(buttons);
        this.findViewById(R.id.text_cast).setVisibility(text);
    }

}
