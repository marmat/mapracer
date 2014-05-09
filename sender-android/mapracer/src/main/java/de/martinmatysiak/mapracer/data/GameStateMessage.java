package de.martinmatysiak.mapracer.data;

import com.google.android.gms.maps.model.LatLng;

public class GameStateMessage extends Message {
    public static final String TYPE = "game_state";

    public int players;
    public String state;
    public Race race;

    public GameStateMessage() {
        super(TYPE);
    }

    public static class Race {
        public LatLng startLocation;
        public long startTime;
        public LatLng targetLocation;
        public String targetTitle;
    }
}
