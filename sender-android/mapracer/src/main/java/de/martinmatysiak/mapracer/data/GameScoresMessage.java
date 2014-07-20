package de.martinmatysiak.mapracer.data;

import java.util.List;

public class GameScoresMessage extends Message {
    public List<PlayerInfo> scores;

    public GameScoresMessage() {
        super(MessageType.GAME_SCORES);
    }

    public static class PlayerInfo {
        public String id;
        public String name;
        public double score;
        public long time;
    }
}
