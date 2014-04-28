package de.martinmatysiak.mapracer;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Just some application wide constants in a single place.
 */
public class Constants {

    private static final String TAG = "Constants";

    public static final String CAST_APP_ID = "807AB2E8";
    public static final String CAST_NAMESPACE = "urn:x-cast:de.martinmatysiak.mapracer";

    public static final String DATA_TYPE = "type";
    public static final String DATA_START_LOCATION = "start_location";
    public static final String DATA_TARGET_LOCATION = "target_location";
    public static final String DATA_TARGET_TITLE = "target_title";

    public static final String INTENT_DEVICE = "device";

    public static final String GAME_REQUEST = "request";
    public static final String GAME_START = "start";
    public static final String GAME_POSITION = "position";
    public static final String GAME_STOP = "stop";


    public static final LatLng DEBUG_START_LOCATION = new LatLng(37.413084, -122.069217);
    public static final LatLng DEBUG_TARGET_LOCATION = new LatLng(37.420283, -122.083961);
    public static final String DEBUG_TARGET_TITLE = "Android";

    // Yeah, the class name Constants isn't quite fitting for what follows. Please ignore.

    public static JSONObject latLngToJson(LatLng latLng) {
        JSONObject o = new JSONObject();
        try {
            o.put("lat", latLng.latitude);
            o.put("lng", latLng.longitude);
        } catch (JSONException e) {
            Log.w(TAG, "Exception in latLngToJson", e);
        }

        return o;
    }

    public static LatLng jsonToLatLng(JSONObject o) {
        if (o.has("lat") && o.has("lng")) {
            try {
                return new LatLng(o.getDouble("lat"), o.getDouble("lng"));
            } catch (JSONException e) {
                Log.w(TAG, "Exception in jsonToLatLng", e);
            }
        }

        return null;
    }
}
