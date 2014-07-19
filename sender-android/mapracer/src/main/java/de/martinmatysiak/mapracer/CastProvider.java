package de.martinmatysiak.mapracer;

import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.common.api.GoogleApiClient;

public interface CastProvider {
    /**
     * The requesting fragment should not do any (dis-)connects, all connection related things must
     * be handled entirely by the providing activity.
     *
     * @return A GoogleApiClient instance or null if not casting.
     */
    public GoogleApiClient getApiClient();

    /**
     * @return The device to which the application is currently casting. Might be null.
     */
    public CastDevice getSelectedDevice();

    /**
     * Indicates that the fragment wants to be informed of changes to the ApiClient. Should call the
     * requesting listener immediately if the Api client is currently non-null.
     *
     * @param listener The ApiClientChange listener.
     */
    public void addOnApiClientChangeListener(OnApiClientChangeListener listener);

    /**
     * Removes a previously registered listener. Does nothing if it wasn't registered previously.
     *
     * @param listener The ApiClientChange listener to remove.
     */
    public void removeOnApiClientChangeListener(OnApiClientChangeListener listener);
}
