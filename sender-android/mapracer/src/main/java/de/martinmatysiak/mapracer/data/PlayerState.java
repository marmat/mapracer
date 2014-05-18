package de.martinmatysiak.mapracer.data;

import android.util.Log;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public enum PlayerState {
    ACTIVE,
    WAITING,
    FINISHED
}

class PlayerStateSerializer implements JsonSerializer<PlayerState>, JsonDeserializer<PlayerState> {

    public static final String TAG = "PlayerStateSerializer";

    public JsonElement serialize(final PlayerState PlayerState, final Type type,
                                 final JsonSerializationContext context) {
        return new JsonPrimitive(PlayerState.name().toLowerCase());
    }

    public PlayerState deserialize(final JsonElement json, final Type type,
                                   final JsonDeserializationContext context) {

        String state = json.getAsString();
        try {
            return PlayerState.valueOf(state.toUpperCase().trim());
        } catch (IllegalArgumentException ex) {
            Log.w(TAG, "Received invalid PlayerState in message: " + state);
            return PlayerState.WAITING;
        }
    }
}