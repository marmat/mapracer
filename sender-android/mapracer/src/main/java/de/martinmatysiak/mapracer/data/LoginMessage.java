package de.martinmatysiak.mapracer.data;

public class LoginMessage extends Message {
    public String id;
    public String name = null;

    public LoginMessage() {
        super(MessageType.LOGIN);
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
