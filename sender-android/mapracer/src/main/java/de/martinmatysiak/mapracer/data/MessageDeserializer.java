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
        String type = object.get("type").getAsString();

        if (type.equals(GameStateMessage.TYPE)) {
            return context.deserialize(json, GameStateMessage.class);
        } else if (type.equals(PlayerStateMessage.TYPE)) {
            return context.deserialize(json, PlayerStateMessage.class);
        } else if (type.equals(GameScoresMessage.TYPE)) {
            return context.deserialize(json, GameScoresMessage.class);
        } else {
            // Unknown type
            Log.w(TAG, "Received unknown message of type: " + type);
            return new Message(type);
        }
    }
}
