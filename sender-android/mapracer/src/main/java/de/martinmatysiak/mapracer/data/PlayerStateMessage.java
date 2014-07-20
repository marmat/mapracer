package de.martinmatysiak.mapracer.data;

public class PlayerStateMessage extends Message {
    public PlayerState state;

    public PlayerStateMessage() {
        super(MessageType.PLAYER_STATE);
    }
}
