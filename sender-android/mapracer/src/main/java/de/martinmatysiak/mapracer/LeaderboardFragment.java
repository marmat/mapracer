package de.martinmatysiak.mapracer;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.common.api.GoogleApiClient;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.martinmatysiak.mapracer.data.GameScoresMessage;
import de.martinmatysiak.mapracer.data.Message;
import de.martinmatysiak.mapracer.data.MessageType;

public class LeaderboardFragment extends ListFragment implements OnApiClientChangeListener, Cast.MessageReceivedCallback {

    public static final String TAG = LeaderboardFragment.class.getSimpleName();

    class LeaderboardAdapter extends ArrayAdapter<GameScoresMessage.PlayerInfo> {
        public LeaderboardAdapter(Context context, List<GameScoresMessage.PlayerInfo> data) {
            super(context, R.layout.leaderboard_item, data);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.leaderboard_item, parent, false);
            }

            GameScoresMessage.PlayerInfo player = getItem(position);
            ((TextView) convertView.findViewById(R.id.position)).setText(Integer.toString(position + 1));
            ((TextView) convertView.findViewById(R.id.name)).setText(player.name);
            if (player.score < 0) {
                ((TextView) convertView.findViewById(R.id.score)).setText(
                        new SimpleDateFormat(Constants.TIME_FORMAT).format(new Date(player.time)));
            }

            return convertView;
        }
    }

    CastProvider mCastProvider;
    LeaderboardAdapter mAdapter;
    List<GameScoresMessage.PlayerInfo> mData = new ArrayList<GameScoresMessage.PlayerInfo>();

    public LeaderboardFragment() {
        super();
    }

    public void setData(List<GameScoresMessage.PlayerInfo> data) {
        mData = data;

        // Clearing and re-adding all elements seems to be the best (or at least simplest) approach.
        if (mAdapter != null) {
            mAdapter.clear();
            mAdapter.addAll(mData);
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(TAG, "onAttach");
        try {
            mCastProvider = (CastProvider) activity;
        } catch (ClassCastException ex) {
            throw new ClassCastException(activity.toString()
                    + " must implement CastProvider");
        }

        mCastProvider.addOnApiClientChangeListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mAdapter = new LeaderboardAdapter(inflater.getContext(), mData);
        setListAdapter(mAdapter);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onApiClientChange(GoogleApiClient apiClient) {
        Log.d(TAG, "onApiClientChange");
        if (apiClient != null && apiClient.isConnected()) {
            mCastProvider.addMessageReceivedCallback(Constants.CAST_NAMESPACE, this);
        }
    }

    @Override
    public void onMessageReceived(CastDevice castDevice, String namespace, String json) {
        Log.d(TAG, "onMessageReceived: " + json);
        Message message = Message.fromJson(json);
        if (message.type == MessageType.GAME_SCORES) {
            setData(((GameScoresMessage) message).scores);
        }
    }
}
