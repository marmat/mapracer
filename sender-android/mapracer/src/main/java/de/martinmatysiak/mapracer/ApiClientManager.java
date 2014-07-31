package de.martinmatysiak.mapracer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.martinmatysiak.mapracer.data.LoginMessage;
import de.martinmatysiak.mapracer.data.LogoutMessage;
import de.martinmatysiak.mapracer.data.Message;

/**
 * A helper class which takes care of all things related to handling the connection to the
 * Google APIs.
 */
public class ApiClientManager
        implements CastProvider,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        ResultCallback<Cast.ApplicationConnectionResult>,
        Cast.MessageReceivedCallback {

    public static final String TAG = ApiClientManager.class.getSimpleName();

    private Context mContext;
    private CastDevice mSelectedDevice;
    private GoogleApiClient mApiClient;
    private SharedPreferences mPreferences;
    private ConnectionStatus mConnectionStatus = ConnectionStatus.NOT_AVAILABLE;
    private List<ConnectionStatusChangeCallback> mConnectionStatusChangeCallbacks = new ArrayList<ConnectionStatusChangeCallback>();
    private Map<String, List<Cast.MessageReceivedCallback>> mMessageReceivedCallbacks = new HashMap<String, List<Cast.MessageReceivedCallback>>();
    private boolean mAutoConnect = false;

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
            setSelectedDevice(null);
            setConnectionStatus(mApiClient.isConnected() ?
                    ConnectionStatus.CONNECTED : ConnectionStatus.DISCONNECTED);
        }
    };

    public ApiClientManager() { /* empty constructor */ }

    public ApiClientManager(Context context) {
        init(context);
    }

    public ApiClientManager(Context context, CastDevice castDevice) {
        init(context, castDevice);
    }

    public void init(Context context) {
        init(context, null);
    }

    public void init(Context context, CastDevice castDevice) {
        mContext = context;
        mPreferences = context.getSharedPreferences(Constants.PREFERENCES, Context.MODE_PRIVATE);
        setSelectedDevice(castDevice);
    }

    public void setSelectedDevice(CastDevice newDevice) {
        Log.d(TAG, "setSelectedDevice:" + (newDevice != null ? newDevice.getDeviceId() : "null"));
        if (mSelectedDevice == newDevice) {
            return;
        }

        CastDevice oldDevice = mSelectedDevice;
        mSelectedDevice = newDevice;

        // If the device changed, we have to reinitialize the API client
        if (oldDevice != null) {
            destroy();
        }

        if (newDevice != null) {
            create();
        }
    }

    private void create() {
        Log.d(TAG, "create");
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
        setConnectionStatus(ConnectionStatus.DISCONNECTED);

        if (mAutoConnect) {
            connect();
        }
    }

    private void destroy() {
        Log.d(TAG, "destroy");
        disconnect(true);

        if (mApiClient != null) {
            // Always disconnect the API client onStop as per Guidelines, otherwise we might end up
            // with "ghost" clients still connected to the cast receiver.
            mApiClient.unregisterConnectionCallbacks(this);
            mApiClient.unregisterConnectionFailedListener(this);
        }

        mApiClient = null;
        setConnectionStatus(ConnectionStatus.NOT_AVAILABLE);
    }

    public void setAutoConnect(boolean autoConnect) {
        mAutoConnect = autoConnect;
    }

    public void connect() {
        Log.d(TAG, "connect");
        if (mApiClient != null && !mApiClient.isConnected()) {
            setConnectionStatus(ConnectionStatus.CONNECTING);
            mApiClient.connect();
        }
    }

    public void disconnect() {
        disconnect(false);
    }

    public void disconnect(boolean logout) {
        Log.d(TAG, "disconnect:" + logout);
        if (mApiClient != null && mApiClient.isConnected()) {
            if (logout) {
                // Indicate that the player won't be coming back anytime soon
                sendMessage(Constants.CAST_NAMESPACE, new LogoutMessage());
                Cast.CastApi.leaveApplication(mApiClient);
            }

            mApiClient.disconnect();
            setConnectionStatus(ConnectionStatus.DISCONNECTED);
        }
    }

    private void setConnectionStatus(ConnectionStatus connectionStatus) {
        if (mConnectionStatus == connectionStatus) {
            return;
        }

        mConnectionStatus = connectionStatus;
        for (ConnectionStatusChangeCallback listener : mConnectionStatusChangeCallbacks) {
            listener.onConnectionStatusChange(connectionStatus);
        }
    }

    @Override
    public CastDevice getSelectedDevice() {
        return mSelectedDevice;
    }

    @Override
    public ConnectionStatus getConnectionStatus() {
        return mConnectionStatus;
    }

    @Override
    public void addConnectionStatusChangeCallback(ConnectionStatusChangeCallback callback) {
        if (!mConnectionStatusChangeCallbacks.contains(callback)) {
            mConnectionStatusChangeCallbacks.add(callback);
        }
    }

    @Override
    public void removeConnectionStatusChangeCallback(ConnectionStatusChangeCallback callback) {
        mConnectionStatusChangeCallbacks.remove(callback);
    }

    @Override
    public PendingResult<Status> sendMessage(String namespace, Message message) {
        return Cast.CastApi.sendMessage(mApiClient, namespace, message.toJson());
    }

    @Override
    public void addMessageReceivedCallback(String namespace, Cast.MessageReceivedCallback callback) {
        if (!mMessageReceivedCallbacks.containsKey(namespace)) {
            // First request for this namespace, subscribe to it ourselves if we're currently
            // casting (otherwise it will be done in onResult).
            if (mConnectionStatus == ConnectionStatus.CASTING) {
                try {
                    Cast.CastApi.setMessageReceivedCallbacks(mApiClient, namespace, this);
                } catch (IOException ex) {
                    Log.w(TAG, "Could not subscribe to channel for " + namespace, ex);
                }
            }

            mMessageReceivedCallbacks.put(namespace, new ArrayList<Cast.MessageReceivedCallback>());
        }

        // Add the callback to the queue
        mMessageReceivedCallbacks.get(namespace).add(callback);
    }

    @Override
    public void removeMessageReceivedCallback(String namespace, Cast.MessageReceivedCallback callback) {
        if (!mMessageReceivedCallbacks.containsKey(namespace)) {
            // invalid request
            return;
        }

        mMessageReceivedCallbacks.get(namespace).remove(callback);
        if (mMessageReceivedCallbacks.get(namespace).size() == 0) {
            // Listening no longer needed, remove ourselves
            if (mConnectionStatus == ConnectionStatus.CASTING) {
                try {
                    Cast.CastApi.removeMessageReceivedCallbacks(mApiClient, namespace);
                } catch (IOException ex) {
                    Log.w(TAG, "Could not remove listener for " + namespace, ex);
                }
            }

            mMessageReceivedCallbacks.remove(namespace);
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        setConnectionStatus(ConnectionStatus.CONNECTED);

        try {
            Cast.CastApi.launchApplication(mApiClient, Constants.CAST_APP_ID, false)
                    .setResultCallback(this);
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch application", e);
        }
    }

    @Override
    public void onResult(Cast.ApplicationConnectionResult result) {
        if (result.getStatus().isSuccess()) {
            // Resubscribe to all watched message channels
            for (String namespace : mMessageReceivedCallbacks.keySet()) {
                try {
                    Cast.CastApi.setMessageReceivedCallbacks(mApiClient, namespace, this);
                } catch (IOException ex) {
                    Log.e(TAG, "Could not subscribe to channel " + namespace, ex);
                }
            }

            // Login with our UUID
            LoginMessage message = new LoginMessage.Builder()
                    .withId(mPreferences.getString(Constants.PREF_UUID, ""))
                    .build();

            sendMessage(Constants.CAST_NAMESPACE, message);
            setConnectionStatus(ConnectionStatus.CASTING);
        } else {
            Log.w(TAG, "ApplicationConnection is not success: " + result.getStatus());
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.w(TAG, "GoogleApi connection suspended: " + cause);
        setConnectionStatus(ConnectionStatus.SUSPENDED);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.w(TAG, "GoogleApi connection failed: " + connectionResult.toString());
        setConnectionStatus(ConnectionStatus.DISCONNECTED);
    }

    @Override
    public void onMessageReceived(CastDevice castDevice, String namespace, String message) {
        // Propagate to all subscribed listeners
        if (mMessageReceivedCallbacks.containsKey(namespace)) {
            for (Cast.MessageReceivedCallback cb : mMessageReceivedCallbacks.get(namespace)) {
                cb.onMessageReceived(castDevice, namespace, message);
            }
        }
    }
}
