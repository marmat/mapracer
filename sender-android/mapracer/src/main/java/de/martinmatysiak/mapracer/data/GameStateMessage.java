package de.martinmatysiak.mapracer.data;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;

public class GameStateMessage extends Message {
    public int players;
    public GameState state;
    public Race race;

    public GameStateMessage() {
        super(MessageType.GAME_STATE);
    }

    public static class Race implements Parcelable {
        public LatLng startLocation;
        public long startTime;
        public LatLng targetLocation;
        public String targetTitle;

        public static final Creator CREATOR = new Creator<Race>() {
            @Override
            public Race createFromParcel(Parcel source) {
                return new Race(source);
            }

            @Override
            public Race[] newArray(int size) {
                return new Race[size];
            }
        };

        public Race() { /* allow empty constructor for GSON */ }

        public Race(Parcel parcel) {
            targetTitle = parcel.readString();
            startTime = parcel.readLong();
            targetLocation = parcel.readParcelable(LatLng.class.getClassLoader());
            startLocation = parcel.readParcelable(LatLng.class.getClassLoader());
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeString(targetTitle);
            parcel.writeLong(startTime);
            parcel.writeParcelable(targetLocation, flags);
            parcel.writeParcelable(startLocation, flags);
        }
    }
}
