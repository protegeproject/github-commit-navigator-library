package edu.stanford.protege.commitnavigator.utils.impl;

import edu.stanford.protege.commitnavigator.config.AuthenticationConfig;
import edu.stanford.protege.commitnavigator.exceptions.AuthenticationException;
import edu.stanford.protege.commitnavigator.utils.AuthenticationManager;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.IOException;
import java.util.Optional;

/**
 * Default implementation of {@link AuthenticationManager} that provides GitHub authentication
 * and credential management services.
 *
 * <p>This implementation supports multiple authentication methods including Personal Access Tokens,
 * OAuth tokens, and username/password combinations. It handles both GitHub API authentication
 * and JGit credential provider creation for Git operations.</p>
 *
 * <p>The class is marked as {@link Singleton} to ensure a single instance is used throughout
 * the application lifecycle.</p>
 *
 * @since 1.0.0
 */
@Singleton
public class AuthenticationManagerImpl implements AuthenticationManager {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationManagerImpl.class);

    /**
     * Authenticates with GitHub using the provided configuration.
     *
     * <p>This method supports the following authentication types:</p>
     * <ul>
     *   <li>Personal Access Token - Uses OAuth token authentication</li>
     *   <li>OAuth Token - Uses OAuth token authentication</li>
     *   <li>Username/Password - Uses password-based authentication</li>
     * </ul>
     *
     * <p>If no configuration is provided, anonymous authentication is attempted
     * for accessing public repositories.</p>
     *
     * @param config the authentication configuration, or null for anonymous access
     * @return an authenticated {@link GitHub} instance
     * @throws AuthenticationException if authentication fails, if the authentication
     *                                 type is not supported, or if SSH key authentication is attempted
     */
    @Override
    public GitHub authenticateGitHub(AuthenticationConfig config) throws AuthenticationException {
        if (config == null) {
            logger.debug("No authentication config provided, connecting to GitHub anonymously");
            try {
                return new GitHubBuilder().build();
            } catch (IOException e) {
                throw new AuthenticationException("Failed to connect to GitHub anonymously", e);
            }
        }

        logger.debug("Authenticating with GitHub using {}", config.getType());

        try {
            var builder = new GitHubBuilder();

            return switch (config.getType()) {
                case PERSONAL_ACCESS_TOKEN, OAUTH -> builder.withOAuthToken(config.getToken()).build();
                case USERNAME_PASSWORD -> builder.withPassword(config.getUsername(), config.getPassword()).build();
                case SSH_KEY -> throw new AuthenticationException("SSH key authentication not supported for GitHub API");
                default -> throw new AuthenticationException("Unsupported authentication type: " + config.getType());
            };
        } catch (IOException e) {
            throw new AuthenticationException("Failed to authenticate with GitHub", e);
        }
    }

    /**
     * Creates a JGit credentials provider for Git operations.
     *
     * <p>This method converts the authentication configuration into a JGit-compatible
     * credentials provider that can be used for Git operations such as clone, fetch, and push.</p>
     *
     * <p>Supported authentication types and their JGit mappings:</p>
     * <ul>
     *   <li>Personal Access Token - Uses token as username with empty password</li>
     *   <li>OAuth Token - Uses token as username with empty password</li>
     *   <li>Username/Password - Uses provided username and password</li>
     * </ul>
     *
     * @param config the authentication configuration, or null for anonymous access
     * @return a {@link CredentialsProvider} for Git operations, or null for anonymous access
     * @throws AuthenticationException if the authentication type is not supported
     *                                 or if SSH key authentication is attempted
     */
    @Override
    public CredentialsProvider getCredentialsProvider(AuthenticationConfig config) throws AuthenticationException {
        if (config == null) {
            logger.debug("No authentication config provided, returning null credentials provider for anonymous access");
            return null;
        }

        logger.debug("Creating credentials provider for {}", config.getType());

        return switch (config.getType()) {
            case PERSONAL_ACCESS_TOKEN, OAUTH -> new UsernamePasswordCredentialsProvider(config.getToken(), "");
            case USERNAME_PASSWORD -> new UsernamePasswordCredentialsProvider(config.getUsername(), config.getPassword());
            case SSH_KEY -> throw new AuthenticationException("SSH key authentication not yet implemented");
            default -> throw new AuthenticationException("Unsupported authentication type: " + config.getType());
        };
    }

    /**
     * Validates the authentication configuration by checking credential format and testing connectivity.
     *
     * <p>This method performs two levels of validation:</p>
     * <ol>
     *   <li>Format validation - Ensures credentials are not null/empty and properly formatted</li>
     *   <li>Connectivity validation - Tests actual authentication with GitHub API</li>
     * </ol>
     *
     * <p>For anonymous access (null configuration), validation is skipped.</p>
     *
     * @param config the authentication configuration to validate, or null for anonymous access
     * @throws AuthenticationException if the configuration is invalid, credentials are malformed,
     *                                 or if authentication with GitHub fails
     */
    @Override
    public void validateAuthentication(AuthenticationConfig config) throws AuthenticationException {
        logger.debug("Validating authentication configuration");

        if (config == null) {
            logger.debug("No authentication config provided, validation skipped for anonymous access");
            return;
        }

        switch (config.getType()) {
            case PERSONAL_ACCESS_TOKEN:
            case OAUTH:
                validateToken(config.getToken());
                break;

            case USERNAME_PASSWORD:
                validateUsernamePassword(config.getUsername(), config.getPassword());
                break;

            case SSH_KEY:
                validateSshKeyPath(config.getSshKeyPath());
                break;

            default:
                throw new AuthenticationException("Unsupported authentication type: " + config.getType());
        }

        try {
            var github = authenticateGitHub(config);
            github.checkApiUrlValidity();
            github.getMyself();
        } catch (IOException e) {
            throw new AuthenticationException("Authentication validation failed", e);
        }
    }

    private void validateToken(String token) throws AuthenticationException {
        if (Optional.ofNullable(token).map(String::trim).filter(t -> !t.isEmpty()).isEmpty()) {
            throw new AuthenticationException("Token cannot be null or empty");
        }
    }

    private void validateUsernamePassword(String username, String password) throws AuthenticationException {
        if (Optional.ofNullable(username).map(String::trim).filter(u -> !u.isEmpty()).isEmpty()) {
            throw new AuthenticationException("Username cannot be null or empty");
        }
        if (Optional.ofNullable(password).map(String::trim).filter(p -> !p.isEmpty()).isEmpty()) {
            throw new AuthenticationException("Password cannot be null or empty");
        }
    }

    private void validateSshKeyPath(String sshKeyPath) throws AuthenticationException {
        if (Optional.ofNullable(sshKeyPath).map(String::trim).filter(path -> !path.isEmpty()).isEmpty()) {
            throw new AuthenticationException("SSH key path cannot be null or empty");
        }
    }
}