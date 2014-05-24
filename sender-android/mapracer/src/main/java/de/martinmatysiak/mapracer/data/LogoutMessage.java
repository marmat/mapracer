package de.martinmatysiak.mapracer.data;

public class LogoutMessage extends Message {
    public static final String TYPE = "logout";

    public LogoutMessage() {
        super(TYPE);
    }

    // This message does not really need a builder, but we add one for consistency
    public static class Builder {
        private LogoutMessage message = new LogoutMessage();

        public LogoutMessage build() {
            return message;
        }
    }
}
