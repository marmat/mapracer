package de.martinmatysiak.mapracer;

public interface ConnectionStatusChangeCallback {
    /**
     * @param connectionStatus The new connection status of the API client.
     */
    public void onConnectionStatusChange(ConnectionStatus connectionStatus);
}
