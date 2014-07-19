package de.martinmatysiak.mapracer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;

import java.util.ArrayList;
import java.util.List;

import de.martinmatysiak.mapracer.data.LoginMessage;
import de.martinmatysiak.mapracer.data.LogoutMessage;

/**
 * A helper class which takes care of all things related to handling the connection to the
 * Google APIs.
 */
public class ApiClientManager
        implements CastProvider,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        ResultCallback<Cast.ApplicationConnectionResult> {

    public static final String TAG = ApiClientManager.class.getSimpleName();

    Context mContext;
    CastDevice mSelectedDevice;
    GoogleApiClient mApiClient;
    SharedPreferences mPreferences;
    List<OnApiClientChangeListener> mOnApiClientChangeListeners;

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

    public ApiClientManager(Context context) {
        mContext = context;
        mPreferences = context.getSharedPreferences(Constants.PREFERENCES, Context.MODE_PRIVATE);
        mOnApiClientChangeListeners = new ArrayList<OnApiClientChangeListener>();
    }

    public ApiClientManager(Context context, CastDevice castDevice) {
        this(context);
        setSelectedDevice(castDevice);
    }

    public void setSelectedDevice(CastDevice device) {
        // If the device changed, we have to reinitialize the API client
        // TODO
        mSelectedDevice = device;
    }

    public void create() {
        if (mSelectedDevice == null) {
            return;
        }

        Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions
                .builder(mSelectedDevice, mCastClientListener);

        mApiClient = new GoogleApiClient.Builder(mContext)
                .addApi(Cast.API, apiOptionsBuilder.build())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        notifyListeners();

        mApiClient.connect();
    }

    public void destroy() {
        destroy(false);
    }

    public void destroy(boolean forceLogout) {
        if (mApiClient != null && mApiClient.isConnected()) {
            // Indicate that the player won't be coming back anytime soon if we are closing the app
            if (forceLogout) {
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
            notifyListeners();
        }
    }

    private void notifyListeners() {
        for (OnApiClientChangeListener listener : mOnApiClientChangeListeners) {
            listener.onApiClientChange(mApiClient);
        }
    }

    @Override
    public GoogleApiClient getApiClient() {
        return mApiClient;
    }

    @Override
    public CastDevice getSelectedDevice() {
        return mSelectedDevice;
    }

    @Override
    public void addOnApiClientChangeListener(OnApiClientChangeListener listener) {
        if (!mOnApiClientChangeListeners.contains(listener)) {
            mOnApiClientChangeListeners.add(listener);
        }

        // Notify new listener immediately if we have a non-null API client.
        if (mApiClient != null) {
            listener.onApiClientChange(mApiClient);
        }
    }

    @Override
    public void removeOnApiClientChangeListener(OnApiClientChangeListener listener) {
        mOnApiClientChangeListeners.remove(listener);
    }

    @Override
    public void onConnected(Bundle bundle) {
        try {
            Cast.CastApi.launchApplication(mApiClient, Constants.CAST_APP_ID, false)
                    .setResultCallback(this);
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch application", e);
        }

        notifyListeners();
    }

    @Override
    public void onResult(Cast.ApplicationConnectionResult result) {
        if (result.getStatus().isSuccess()) {
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
    public void onConnectionSuspended(int cause) {
        Log.w(TAG, "GoogleApi connection suspended: " + cause);
        notifyListeners();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.w(TAG, "GoogleApi connection failed: " + connectionResult.toString());
        notifyListeners();
    }
}
