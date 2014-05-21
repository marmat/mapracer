package de.martinmatysiak.mapracer.data;

public class LoginMessage extends Message {
    public static final String TYPE = "login";

    public String id;
    public String name = null;

    public LoginMessage() {
        super(TYPE);
    }

    public static class Builder {
        private LoginMessage message = new LoginMessage();

        public Builder withId(String id) {
            message.id = id;
            return this;
        }

        public Builder withName(String name) {
            message.name = name;
            return this;
        }

        public LoginMessage build() {
            return message;
        }
    }
}
