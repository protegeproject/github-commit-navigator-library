package edu.stanford.protege.commitnavigator.config;

import java.util.Objects;

/**
 * Configuration class for GitHub authentication credentials and methods.
 *
 * <p>This class encapsulates various authentication mechanisms supported by the GitHub API,
 * including Personal Access Tokens, OAuth tokens, username/password combinations, and SSH keys.
 * The configuration is immutable and uses the builder pattern for construction.</p>
 *
 * <p>Usage examples:</p>
 * <pre>
 * {@code
 * // Personal Access Token
 * var config = AuthenticationConfig.personalAccessToken("ghp_xxxxxxxxxxxx").build();
 *
 * // OAuth Token
 * var config = AuthenticationConfig.oauth("oauth_token").build();
 *
 * // Username/Password
 * var config = AuthenticationConfig.usernamePassword("username", "password").build();
 *
 * // SSH Key
 * var config = AuthenticationConfig.sshKey("/path/to/private/key").build();
 * }
 * </pre>
 *
 * @since 1.0.0
 */
public class AuthenticationConfig {
    private final AuthenticationType type;
    private final String token;
    private final String username;
    private final String password;
    private final String sshKeyPath;

    private AuthenticationConfig(Builder builder) {
        this.type = Objects.requireNonNull(builder.type, "Authentication type cannot be null");
        this.token = builder.token;
        this.username = builder.username;
        this.password = builder.password;
        this.sshKeyPath = builder.sshKeyPath;
    }

    /**
     * Returns the authentication type configured for this instance.
     *
     * @return the {@link AuthenticationType} enum value
     */
    public AuthenticationType getType() {
        return type;
    }

    /**
     * Returns the authentication token (Personal Access Token or OAuth token).
     *
     * @return the token string, or null if not applicable for the authentication type
     */
    public String getToken() {
        return token;
    }

    /**
     * Returns the username for username/password authentication.
     *
     * @return the username string, or null if not applicable for the authentication type
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the password for username/password authentication.
     *
     * @return the password string, or null if not applicable for the authentication type
     */
    public String getPassword() {
        return password;
    }

    /**
     * Returns the SSH private key file path for SSH key authentication.
     *
     * @return the SSH key file path, or null if not applicable for the authentication type
     */
    public String getSshKeyPath() {
        return sshKeyPath;
    }

    /**
     * Creates a builder for Personal Access Token authentication.
     *
     * @param token the GitHub Personal Access Token
     * @return a {@link Builder} instance configured for Personal Access Token authentication
     * @throws NullPointerException if token is null
     */
    public static Builder personalAccessToken(String token) {
        return new Builder(AuthenticationType.PERSONAL_ACCESS_TOKEN, token);
    }

    /**
     * Creates a builder for OAuth token authentication.
     *
     * @param token the OAuth token
     * @return a {@link Builder} instance configured for OAuth authentication
     * @throws NullPointerException if token is null
     */
    public static Builder oauth(String token) {
        return new Builder(AuthenticationType.OAUTH, token);
    }

    /**
     * Creates a builder for username/password authentication.
     *
     * @param username the GitHub username
     * @param password the GitHub password or personal access token
     * @return a {@link Builder} instance configured for username/password authentication
     * @throws NullPointerException if username or password is null
     */
    public static Builder usernamePassword(String username, String password) {
        return new Builder(AuthenticationType.USERNAME_PASSWORD, username, password);
    }

    /**
     * Creates a builder for SSH key authentication.
     *
     * @param sshKeyPath the path to the SSH private key file
     * @return a {@link Builder} instance configured for SSH key authentication
     * @throws NullPointerException if sshKeyPath is null
     */
    public static Builder sshKey(String sshKeyPath) {
        var builder = new Builder(AuthenticationType.SSH_KEY);
        builder.sshKeyPath = sshKeyPath;
        return builder;
    }

    /**
     * Enumeration of supported authentication types.
     */
    public enum AuthenticationType {
        /** GitHub Personal Access Token authentication */
        PERSONAL_ACCESS_TOKEN,
        /** OAuth token authentication */
        OAUTH,
        /** Username and password authentication */
        USERNAME_PASSWORD,
        /** SSH key authentication */
        SSH_KEY
    }

    /**
     * Builder class for constructing {@link AuthenticationConfig} instances.
     */
    public static class Builder {
        private final AuthenticationType type;
        private String token;
        private String username;
        private String password;
        private String sshKeyPath;

        private Builder(AuthenticationType type) {
            this.type = type;
        }

        private Builder(AuthenticationType type, String token) {
            this.type = type;
            this.token = token;
        }

        private Builder(AuthenticationType type, String username, String password) {
            this.type = type;
            this.username = username;
            this.password = password;
        }

        /**
         * Builds an {@link AuthenticationConfig} instance with the configured parameters.
         *
         * @return a new {@link AuthenticationConfig} instance
         */
        public AuthenticationConfig build() {
            return new AuthenticationConfig(this);
        }
    }
}