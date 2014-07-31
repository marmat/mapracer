package de.martinmatysiak.mapracer;

import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;

import de.martinmatysiak.mapracer.data.Message;

public interface CastProvider {
    /**
     * @return The device to which the application is currently casting. Might be null.
     */
    public CastDevice getSelectedDevice();

    /**
     * @return The connection status of the internally managed API client.
     */
    public ConnectionStatus getConnectionStatus();

    /**
     * Indicates that the fragment wants to be informed of changes to the connection status.
     *
     * @param callback The ConnectionStatusChange callback to add.
     */
    public void addConnectionStatusChangeCallback(ConnectionStatusChangeCallback callback);

    /**
     * Removes a previously registered callback. Does nothing if it wasn't registered previously.
     *
     * @param callback The ConnectionStatusChange callback to remove.
     */
    public void removeConnectionStatusChangeCallback(ConnectionStatusChangeCallback callback);

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

    /**
     * Sends the given message via the CastApi or discards it if the client is currently
     * not casting.
     *
     * @param namespace The namespace for which to send a message.
     * @param message   The message to send.
     * @return The CastApi's response.
     */
    public PendingResult<Status> sendMessage(String namespace, Message message);
}
