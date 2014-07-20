package de.martinmatysiak.mapracer.data;

public class LogoutMessage extends Message {
    public LogoutMessage() {
        super(MessageType.LOGOUT);
    }

    // This message does not really need a builder, but we add one for consistency
    public static class Builder {
        private LogoutMessage message = new LogoutMessage();

        public LogoutMessage build() {
            return message;
        }
    }
}
