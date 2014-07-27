package de.martinmatysiak.mapracer;

import com.google.android.gms.cast.Cast;
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

    /**
     * Adds the given callback to be notified in case of messages for the given namespace. Do not
     * call CastApi.setMessageReceivedCallback directly as there can be only one listener at a time
     * which would break usage by multiple fragments or clients.
     *
     * @param namespace The namespace to listen for.
     * @param callback  The callback function to call.
     */
    public void addMessageReceivedCallback(String namespace, Cast.MessageReceivedCallback callback);

    /**
     * Removes the given callback from watching for the given namespace.
     *
     * @param namespace The namespace from which to remove the callback.
     * @param callback  The callback function to remove.
     */
    public void removeMessageReceivedCallback(String namespace, Cast.MessageReceivedCallback callback);
}
