package de.martinmatysiak.mapracer.data;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Modifier;

/**
 * A generic message that can be passed to the Cast device.
 */
public class Message {

    public final String type;

    // A pre-configured Gson instance that is able to (de)serialize MapRacer Messages
    private static final Gson gsonInstance = new GsonBuilder()
            .excludeFieldsWithModifiers(Modifier.STATIC)
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .registerTypeAdapter(Message.class, new MessageDeserializer())
            .registerTypeAdapter(LatLng.class, new LatLngSerializer())
            .create();

    public Message(String type) {
        this.type = type;
    }

    public static Gson getConfiguredGson() {
        return gsonInstance;
    }
}
