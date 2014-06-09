package de.martinmatysiak.mapracer.data;

import java.util.List;

public class GameScoresMessage extends Message {
    public static final String TYPE = "game_scores";

    public List<PlayerInfo> scores;

    public GameScoresMessage() {
        super(TYPE);
    }

    public static class PlayerInfo {
        public String id;
        public String name;
        public double score;
        public long time;
    }
}
