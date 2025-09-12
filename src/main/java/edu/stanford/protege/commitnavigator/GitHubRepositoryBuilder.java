package edu.stanford.protege.commitnavigator;

import edu.stanford.protege.commitnavigator.config.AuthenticationConfig;
import edu.stanford.protege.commitnavigator.config.RepositoryConfig;
import edu.stanford.protege.commitnavigator.impl.GitHubRepositoryImpl;
import edu.stanford.protege.commitnavigator.model.RepositoryCoordinates;
import edu.stanford.protege.commitnavigator.utils.AuthenticationManager;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Builder class for creating {@link GitHubRepository} instances with flexible configuration
 * options.
 *
 * <p>This builder provides a fluent API for configuring repository parameters including
 * authentication methods, branch selection, and clone behavior. The builder supports multiple
 * authentication mechanisms.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * var navigator = GitHubRepoNavigatorBuilder
 *     .forRepository("https://github.com/user/repo.git")
 *     .withPersonalAccessToken("ghp_xxxxxxxxxxxx")
 *     .localCloneDirectory("/tmp/repo")
 *     .branch("develop")
 *     .shallowClone(true)
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public class GitHubRepositoryBuilder {
  private final RepositoryConfig.Builder configBuilder;

  private final AuthenticationManager authManager;

  public GitHubRepositoryBuilder(
      RepositoryCoordinates repositoryCoordinates, AuthenticationManager authManager) {
    this.configBuilder = RepositoryConfig.builder(repositoryCoordinates);
    this.authManager = Objects.requireNonNull(authManager, "Authentication manager cannot be null");
  }

  /**
   * Configures authentication using a GitHub Personal Access Token.
   *
   * @param token the GitHub Personal Access Token
   * @return this builder instance for method chaining
   */
  public GitHubRepositoryBuilder withPersonalAccessToken(String token) {
    configBuilder.authConfig(AuthenticationConfig.personalAccessToken(token).build());
    return this;
  }

  /**
   * Configures authentication using an OAuth token.
   *
   * @param token the OAuth token
   * @return this builder instance for method chaining
   */
  public GitHubRepositoryBuilder withOAuthToken(String token) {
    configBuilder.authConfig(AuthenticationConfig.oauth(token).build());
    return this;
  }

  /**
   * Configures authentication using username and password credentials.
   *
   * @param username the GitHub username
   * @param password the GitHub password or personal access token
   * @return this builder instance for method chaining
   */
  public GitHubRepositoryBuilder withUsernamePassword(String username, String password) {
    configBuilder.authConfig(AuthenticationConfig.usernamePassword(username, password).build());
    return this;
  }

  /**
   * Configures authentication using SSH key authentication.
   *
   * @param sshKeyPath the path to the SSH private key file
   * @return this builder instance for method chaining
   */
  public GitHubRepositoryBuilder withSshKey(String sshKeyPath) {
    configBuilder.authConfig(AuthenticationConfig.sshKey(sshKeyPath).build());
    return this;
  }

  /**
   * Specifies the local directory where the repository will be cloned.
   *
   * @param path the local directory path as a string
   * @return this builder instance for method chaining
   */
  public GitHubRepositoryBuilder localCloneDirectory(String path) {
    configBuilder.localCloneDirectory(Paths.get(path));
    return this;
  }

  /**
   * Specifies the local directory where the repository will be cloned.
   *
   * @param path the local directory path as a {@link Path} object
   * @return this builder instance for method chaining
   */
  public GitHubRepositoryBuilder localCloneDirectory(Path path) {
    configBuilder.localCloneDirectory(path);
    return this;
  }

  /**
   * Configures whether to perform a shallow clone of the repository.
   *
   * <p>Shallow clones only download the latest commit, reducing clone time and disk usage, but
   * limiting navigation to recent history.
   *
   * @param shallow true to perform shallow clone, false for full clone
   * @return this builder instance for method chaining
   */
  public GitHubRepositoryBuilder shallowClone(boolean shallow) {
    configBuilder.shallowClone(shallow);
    return this;
  }

  /**
   * Builds a {@link GitHubRepository} instance with the configured parameters.
   *
   * <p>Authentication is optional - if no authentication methods are called, the navigator will
   * attempt to access public repositories without authentication.
   *
   * @return a configured {@link GitHubRepository} instance
   * @throws IllegalStateException if required configuration parameters are missing
   */
  public GitHubRepository build() {
    var config = configBuilder.build();
    return new GitHubRepositoryImpl(config, authManager);
  }
}
