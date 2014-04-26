package de.martinmatysiak.mapracer;

import com.google.android.gms.maps.model.LatLng;

/**
 * Just some application wide constants in a single place.
 */
public class Constants {

    public static final String CAST_APP_ID = "807AB2E8";
    public static final String CAST_NAMESPACE = "urn:x-cast:de.martinmatysiak.mapracer";

    public static final String INTENT_DEVICE = "device";
    public static final String INTENT_START_LOCATION = "start_location";
    public static final String INTENT_TARGET_LOCATION = "target_location";
    public static final String INTENT_TARGET_TITLE = "target_title";

    public static final LatLng DEBUG_START_LOCATION = new LatLng(37.413084, -122.069217);
    public static final LatLng DEBUG_TARGET_LOCATION = new LatLng(37.420283, -122.083961);
    public static final String DEBUG_TARGET_TITLE = "Android";

}