package de.martinmatysiak.mapracer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.ArrayList;
import java.util.List;

import de.martinmatysiak.mapracer.data.GameState;
import de.martinmatysiak.mapracer.data.GameStateMessage;


/**
 * MapActivity is really just a wrapper for the RaceFragment. It is used if we want to show the
 * StreetView UI in a separate Activity (e.g. on small screens). In case of a tablet, the
 * RaceFragment might be used directly in the main activity (i.e. side by side with the
 * leaderboard). The activity just makes sure to be able to provide an ApiClient to the Fragment,
 * everything else will be taken care of in the Fragment.
 */
public class MapActivity extends Activity implements CastProvider {

    public static final String TAG = MapActivity.class.getSimpleName();

    ApiClientManager mApiClientManager;
    List<OnApiClientChangeListener> mListenerBuffer = new ArrayList<OnApiClientChangeListener>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        RaceFragment race = (RaceFragment) getFragmentManager().findFragmentById(R.id.raceFragment);
        Intent intent = getIntent();

        mApiClientManager = new ApiClientManager(this,
                (CastDevice) intent.getParcelableExtra(Constants.INTENT_DEVICE));

        // Make sure to register all buffered listeners
        for (OnApiClientChangeListener listener : mListenerBuffer) {
            mApiClientManager.addOnApiClientChangeListener(listener);
        }
        mListenerBuffer.clear();

        race.setRace((GameStateMessage.Race) intent.getParcelableExtra(Constants.INTENT_RACE));
        race.setState((GameState) intent.getSerializableExtra(Constants.INTENT_STATE));
    }

    @Override
    protected void onStart() {
        super.onStart();
        mApiClientManager.create();
    }

    @Override
    protected void onStop() {
        mApiClientManager.destroy();
        super.onStop();
    }

    @Override
    public GoogleApiClient getApiClient() {
        return mApiClientManager.getApiClient();
    }

    @Override
    public CastDevice getSelectedDevice() {
        return mApiClientManager.getSelectedDevice();
    }

    @Override
    public void addOnApiClientChangeListener(OnApiClientChangeListener listener) {
        // Delay the listener registration in case we're not ready yet
        if (mApiClientManager == null) {
            mListenerBuffer.add(listener);
        } else {
            mApiClientManager.addOnApiClientChangeListener(listener);
        }
    }

    @Override
    public void removeOnApiClientChangeListener(OnApiClientChangeListener listener) {
        if (mApiClientManager == null) {
            mListenerBuffer.remove(listener);
        } else {
            mApiClientManager.removeOnApiClientChangeListener(listener);
        }
    }
}
