package de.martinmatysiak.mapracer;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.StreetViewPanoramaFragment;
import com.google.android.gms.maps.model.StreetViewPanoramaLocation;

import de.martinmatysiak.mapracer.data.GameState;
import de.martinmatysiak.mapracer.data.GameStateMessage;
import de.martinmatysiak.mapracer.data.Message;
import de.martinmatysiak.mapracer.data.PlayerState;
import de.martinmatysiak.mapracer.data.PlayerStateMessage;
import de.martinmatysiak.mapracer.data.PositionMessage;


/**
 * The RaceFragment handles all logic needed during an active race, i.e. passing along the user
 * input to the Cast Receiver and processing State updates.
 */
public class RaceFragment extends StreetViewPanoramaFragment implements
        Cast.MessageReceivedCallback,
        StreetViewPanorama.OnStreetViewPanoramaChangeListener {

    final static String TAG = RaceFragment.class.getSimpleName();

    CastProvider mCastProvider;
    GameStateMessage.Race mRace;
    SharedPreferences mPreferences;
    StreetViewPanorama mPanorama;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param race   The race that this fragment will be used in.
     * @param state  The current game state.
     * @param device The receiver with which we are connected.
     * @return A new instance of fragment RaceFragment.
     */
    public static RaceFragment newInstance(GameStateMessage.Race race, GameState state, CastDevice device) {
        RaceFragment fragment = new RaceFragment();
        Bundle args = new Bundle();
        args.putParcelable(Constants.INTENT_RACE, race);
        args.putParcelable(Constants.INTENT_DEVICE, device);
        args.putSerializable(Constants.INTENT_STATE, state);
        fragment.setArguments(args);
        return fragment;
    }

    public RaceFragment() { /* Required empty public constructor */ }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPreferences = getActivity().getSharedPreferences(Constants.PREFERENCES, Context.MODE_PRIVATE);
        if (getArguments() != null) {
            setRace((GameStateMessage.Race) getArguments().getParcelable(Constants.INTENT_RACE));
            setState((GameState) getArguments().getSerializable(Constants.INTENT_STATE));
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCastProvider = (CastProvider) activity;
        } catch (ClassCastException ex) {
            throw new ClassCastException(activity.toString()
                    + " must implement CastProvider");
        }

        mCastProvider.addMessageReceivedCallback(Constants.CAST_NAMESPACE, this);
    }

    public void setRace(GameStateMessage.Race race) {
        mRace = race;

        // (Re)initialize the StreetViewPanorama
        mPanorama = getStreetViewPanorama();
        mPanorama.setPosition(mRace.startLocation);
        mPanorama.setOnStreetViewPanoramaChangeListener(this);
    }

    public void setState(GameState state) {
        switch (state) {
            case RACE:
                ((Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(Constants.VIBRATE_DURATION);
                // no break on purpose!
            case LOAD:
                if (mPanorama != null) {
                    mPanorama.setUserNavigationEnabled(state == GameState.RACE);
                }
                break;
            case SCORES:
            case INIT:
                // ??? indicate that the fragment can be closed ???
                break;
        }
    }

    @Override
    public void onMessageReceived(CastDevice castDevice, String namespace, String json) {
        Log.d(TAG, "onMessageReceived: " + json);
        Message message = Message.fromJson(json);
        switch (message.type) {
            case GAME_STATE:
                setState(((GameStateMessage) message).state);
                break;
            case PLAYER_STATE:
                if (((PlayerStateMessage) message).state == PlayerState.FINISHED) {
                    Toast.makeText(getActivity(), "You've finished the race!", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    @Override
    public void onStreetViewPanoramaChange(StreetViewPanoramaLocation location) {
        if (mCastProvider.getConnectionStatus() != ConnectionStatus.CASTING) {
            return;
        }

        PositionMessage message = new PositionMessage.Builder()
                .withLocation(location.position)
                .build();
        mCastProvider.sendMessage(Constants.CAST_NAMESPACE, message);
    }
}
