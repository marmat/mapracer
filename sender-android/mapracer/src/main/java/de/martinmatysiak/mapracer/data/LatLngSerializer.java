package de.martinmatysiak.mapracer.data;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class LatLngSerializer implements JsonSerializer<LatLng>, JsonDeserializer<LatLng> {

    public JsonElement serialize(final LatLng latLng, final Type type,
                                 final JsonSerializationContext context) {

        JsonObject result = new JsonObject();
        result.addProperty("lat", latLng.latitude);
        result.addProperty("lng", latLng.longitude);

        return result;
    }

    public LatLng deserialize(final JsonElement json, final Type type,
                              final JsonDeserializationContext context) {

        JsonObject object = json.getAsJsonObject();
        return new LatLng(object.get("lat").getAsDouble(), object.get("lng").getAsDouble());
    }
}
