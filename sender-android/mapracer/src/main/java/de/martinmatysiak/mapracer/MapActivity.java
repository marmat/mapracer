package de.martinmatysiak.mapracer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.cast.CastDevice;

import de.martinmatysiak.mapracer.data.GameState;
import de.martinmatysiak.mapracer.data.GameStateMessage;


/**
 * MapActivity is really just a wrapper for the RaceFragment. It is used if we want to show the
 * StreetView UI in a separate Activity (e.g. on small screens). In case of a tablet, the
 * RaceFragment might be used directly in the main activity (i.e. side by side with the
 * leaderboard).
 */
public class MapActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        RaceFragment race = (RaceFragment) getFragmentManager().findFragmentById(R.id.raceFragment);
        Intent intent = getIntent();

        race.setDevice((CastDevice) intent.getParcelableExtra(Constants.INTENT_DEVICE));
        race.setRace((GameStateMessage.Race) intent.getParcelableExtra(Constants.INTENT_RACE));
        race.setState((GameState) intent.getSerializableExtra(Constants.INTENT_STATE));
    }

}
