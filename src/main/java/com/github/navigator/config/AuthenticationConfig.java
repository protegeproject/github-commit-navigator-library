package com.github.navigator.config;

public class AuthenticationConfig {
    private final AuthenticationType type;
    private final String token;
    private final String username;
    private final String password;
    private final String sshKeyPath;

    private AuthenticationConfig(Builder builder) {
        this.type = builder.type;
        this.token = builder.token;
        this.username = builder.username;
        this.password = builder.password;
        this.sshKeyPath = builder.sshKeyPath;
    }

    public AuthenticationType getType() {
        return type;
    }

    public String getToken() {
        return token;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getSshKeyPath() {
        return sshKeyPath;
    }

    public static Builder personalAccessToken(String token) {
        return new Builder(AuthenticationType.PERSONAL_ACCESS_TOKEN, token);
    }

    public static Builder oauth(String token) {
        return new Builder(AuthenticationType.OAUTH, token);
    }

    public static Builder usernamePassword(String username, String password) {
        return new Builder(AuthenticationType.USERNAME_PASSWORD, username, password);
    }

    public static Builder sshKey(String sshKeyPath) {
        return new Builder(AuthenticationType.SSH_KEY, sshKeyPath);
    }

    public enum AuthenticationType {
        PERSONAL_ACCESS_TOKEN,
        OAUTH,
        USERNAME_PASSWORD,
        SSH_KEY
    }

    public static class Builder {
        private final AuthenticationType type;
        private String token;
        private String username;
        private String password;
        private String sshKeyPath;

        private Builder(AuthenticationType type, String token) {
            this.type = type;
            this.token = token;
        }

        private Builder(AuthenticationType type, String username, String password) {
            this.type = type;
            this.username = username;
            this.password = password;
        }

        public AuthenticationConfig build() {
            return new AuthenticationConfig(this);
        }
    }
}