package de.martinmatysiak.mapracer.data;

public class PlayerStateMessage extends Message {
    public static final String TYPE = "player_state";

    public String state = "";

    public PlayerStateMessage() {
        super(TYPE);
    }
}
