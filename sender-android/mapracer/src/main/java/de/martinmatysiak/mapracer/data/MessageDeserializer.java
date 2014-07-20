package de.martinmatysiak.mapracer.data;

import android.util.Log;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.lang.reflect.Type;


public class MessageDeserializer implements JsonDeserializer<Message> {

    public static final String TAG = "MessageDeserializer";

    public Message deserialize(final JsonElement json, final Type typeOf,
                               final JsonDeserializationContext context) {

        JsonObject object = json.getAsJsonObject();
        MessageType type = context.deserialize(object.get("type"), MessageType.class);

        switch (type) {
            case GAME_STATE:
                return context.deserialize(json, GameStateMessage.class);
            case PLAYER_STATE:
                return context.deserialize(json, PlayerStateMessage.class);
            case GAME_SCORES:
                return context.deserialize(json, GameScoresMessage.class);
            default:
                Log.w(TAG, "Received unexpected message of type: " + type);
                return new Message(type);
        }
    }
}
