package de.martinmatysiak.mapracer.data;

import com.google.android.gms.maps.model.LatLng;

public class RequestMessage extends Message {
    public String targetTitle;
    public LatLng coarseUserLocation;
    public LatLng targetLocation;
    public LatLng startLocation;

    public RequestMessage() {
        super(MessageType.REQUEST);
    }

    public static class Builder {
        private RequestMessage message = new RequestMessage();

        public Builder withTarget(String title, LatLng location) {
            message.targetTitle = title;
            message.targetLocation = location;
            return this;
        }

        public Builder withStart(LatLng location) {
            message.startLocation = location;
            return this;
        }

        public Builder withUserLocation(LatLng location) {
            message.coarseUserLocation = location;
            return this;
        }

        public RequestMessage build() {
            return message;
        }
    }
}
