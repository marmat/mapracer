package de.martinmatysiak.mapracer;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;


public class MapActivity
        extends Activity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    final static String TAG = "MapActivity";

    CastDevice mSelectedDevice;
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
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Initialize the maps view
        GoogleMap map = ((MapFragment) getFragmentManager()
                .findFragmentById(R.id.map)).getMap();

        LatLng start = getIntent().getParcelableExtra(Constants.INTENT_START_LOCATION);

        map.setMyLocationEnabled(false);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(start, 13));

        map.addMarker(new MarkerOptions()
                .title("Start")
                .snippet("Ready, Set, Go!")
                .position(start));

        // Reconnect to the cast device and enable communication
        mSelectedDevice = this.getIntent().getParcelableExtra(Constants.INTENT_DEVICE);
        Log.d(TAG, "Device: " + mSelectedDevice.getDeviceId());

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
            // TODO: Connect to the communication channel as well
            Cast.CastApi.launchApplication(mApiClient, Constants.CAST_APP_ID, false);
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
}
