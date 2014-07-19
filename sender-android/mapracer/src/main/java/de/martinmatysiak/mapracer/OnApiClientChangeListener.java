package de.martinmatysiak.mapracer;

import com.google.android.gms.common.api.GoogleApiClient;

public interface OnApiClientChangeListener {
    /**
     * @param apiClient The new or currently active ApiClient. May be null.
     *                  TODO: maybe add reason param?
     */
    public void onApiClientChange(GoogleApiClient apiClient);
}
