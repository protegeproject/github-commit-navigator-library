package edu.stanford.protege.commitnavigator.utils;

import edu.stanford.protege.commitnavigator.config.AuthenticationConfig;
import edu.stanford.protege.commitnavigator.exceptions.AuthenticationException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.kohsuke.github.GitHub;

/**
 * Service interface for managing GitHub authentication and credentials.
 *
 * <p>This interface provides methods to authenticate with GitHub using various
 * authentication methods including Personal Access Tokens, OAuth tokens,
 * username/password combinations, and SSH keys. It also handles credential
 * provider creation for Git operations.</p>
 *
 * @since 1.0.0
 */
public interface AuthenticationManager {
    /**
     * Authenticates with GitHub using the provided authentication configuration.
     *
     * @param config the authentication configuration containing credentials and method
     * @return a authenticated {@link GitHub} instance
     * @throws AuthenticationException if authentication fails or credentials are invalid
     */
    GitHub authenticateGitHub(AuthenticationConfig config) throws AuthenticationException;
    /**
     * Creates a JGit credentials provider for Git operations using the authentication configuration.
     *
     * @param config the authentication configuration containing credentials and method
     * @return a {@link CredentialsProvider} for Git operations
     * @throws AuthenticationException if the authentication configuration is invalid or unsupported
     */
    CredentialsProvider getCredentialsProvider(AuthenticationConfig config) throws AuthenticationException;
    /**
     * Validates the authentication configuration by attempting to authenticate with GitHub.
     *
     * @param config the authentication configuration to validate
     * @throws AuthenticationException if the authentication configuration is invalid,
     *                                 credentials are incorrect, or authentication fails
     */
    void validateAuthentication(AuthenticationConfig config) throws AuthenticationException;
}