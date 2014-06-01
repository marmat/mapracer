package de.martinmatysiak.mapracer;

import android.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.martinmatysiak.mapracer.data.GameScoresMessage;

public class LeaderboardFragment extends ListFragment {
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
                // TODO show time
                ((TextView) convertView.findViewById(R.id.score)).setText("finished");
            }

            return convertView;
        }
    }

    LeaderboardAdapter mAdapter;
    List<GameScoresMessage.PlayerInfo> mData = new ArrayList<GameScoresMessage.PlayerInfo>();

    public LeaderboardFragment() {
        super();
    }

    public void setData(List<GameScoresMessage.PlayerInfo> data) {
        mData = data;
        if (mAdapter != null) {
            // TODO that's probably not the best way to do it...
            mAdapter.clear();
            mAdapter.addAll(mData);
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mAdapter = new LeaderboardAdapter(inflater.getContext(), mData);
        setListAdapter(mAdapter);
        return super.onCreateView(inflater, container, savedInstanceState);
    }
}
