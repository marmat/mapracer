package de.martinmatysiak.mapracer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;

import de.martinmatysiak.mapracer.data.GameState;
import de.martinmatysiak.mapracer.data.GameStateMessage;
import de.martinmatysiak.mapracer.data.Message;


/**
 * MapActivity is really just a wrapper for the RaceFragment. It is used if we want to show the
 * StreetView UI in a separate Activity (e.g. on small screens). In case of a tablet, the
 * RaceFragment might be used directly in the main activity (i.e. side by side with the
 * leaderboard). The activity just makes sure to be able to provide an ApiClient to the Fragment,
 * everything else will be taken care of in the Fragment.
 */
public class MapActivity extends Activity implements CastProvider {

    public static final String TAG = MapActivity.class.getSimpleName();

    ApiClientManager mApiClientManager = new ApiClientManager();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        RaceFragment race = (RaceFragment) getFragmentManager().findFragmentById(R.id.raceFragment);
        Intent intent = getIntent();

        mApiClientManager.init(this, (CastDevice) intent.getParcelableExtra(Constants.INTENT_DEVICE));

        race.setRace((GameStateMessage.Race) intent.getParcelableExtra(Constants.INTENT_RACE));
        race.setState((GameState) intent.getSerializableExtra(Constants.INTENT_STATE));
    }

    @Override
    protected void onStart() {
        super.onStart();
        mApiClientManager.setAutoConnect(true);
        mApiClientManager.connect();
    }

    @Override
    protected void onStop() {
        mApiClientManager.setAutoConnect(false);
        mApiClientManager.disconnect();
        super.onStop();
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
}
