package de.martinmatysiak.mapracer.data;

import android.util.Log;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public enum MessageType {
    GAME_SCORES,
    GAME_STATE,
    LOGIN,
    LOGOUT,
    PLAYER_STATE,
    POSITION,
    REQUEST,
    UNKNOWN
}

class MessageTypeSerializer implements JsonSerializer<MessageType>, JsonDeserializer<MessageType> {

    public static final String TAG = "MessageTypeSerializer";

    public JsonElement serialize(final MessageType messageType, final Type type,
                                 final JsonSerializationContext context) {
        return new JsonPrimitive(messageType.name().toLowerCase());
    }

    public MessageType deserialize(final JsonElement json, final Type type,
                                   final JsonDeserializationContext context) {

        String messageType = json.getAsString();
        try {
            return MessageType.valueOf(messageType.toUpperCase().trim());
        } catch (IllegalArgumentException ex) {
            Log.w(TAG, "Received invalid MessageType in message: " + messageType);
            return MessageType.UNKNOWN;
        }
    }
}