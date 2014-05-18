package de.martinmatysiak.mapracer.data;

//public final class GameState {
//    private GameState() { /* non-instantiable */ }
//
//    public static final String INIT = "init";
//    public static final String LOAD = "load";
//    public static final String RACE = "race";
//    public static final String SCORES = "scores";
//}

import android.util.Log;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public enum GameState {
    INIT,
    LOAD,
    RACE,
    SCORES
}

class GameStateSerializer implements JsonSerializer<GameState>, JsonDeserializer<GameState> {

    public static final String TAG = "GameStateSerializer";

    public JsonElement serialize(final GameState gameState, final Type type,
                                 final JsonSerializationContext context) {
        return new JsonPrimitive(gameState.name().toLowerCase());
    }

    public GameState deserialize(final JsonElement json, final Type type,
                                 final JsonDeserializationContext context) {

        String state = json.getAsString();
        try {
            return GameState.valueOf(state.toUpperCase().trim());
        } catch (IllegalArgumentException ex) {
            Log.w(TAG, "Received invalid GameState in message: " + state);
            return GameState.INIT;
        }
    }
}