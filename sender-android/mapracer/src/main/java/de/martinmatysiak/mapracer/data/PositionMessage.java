package de.martinmatysiak.mapracer.data;

import com.google.android.gms.maps.model.LatLng;

public class PositionMessage extends Message {
    public static final String TYPE = "position";

    public LatLng location;

    public PositionMessage() {
        super(TYPE);
    }

    public static class Builder {
        private PositionMessage message = new PositionMessage();

        public Builder withLocation(LatLng location) {
            message.location = location;
            return this;
        }

        public PositionMessage build() {
            return message;
        }
    }
}
