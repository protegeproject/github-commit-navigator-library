package edu.stanford.protege.commitnavigator;

import edu.stanford.protege.commitnavigator.config.AuthenticationConfig;
import edu.stanford.protege.commitnavigator.config.RepositoryConfig;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Builder class for creating {@link GitHubRepository} instances with flexible configuration options.
 * 
 * <p>This builder provides a fluent API for configuring repository navigation parameters including
 * authentication methods, file filters, branch selection, and clone behavior. The builder supports
 * multiple authentication mechanisms and allows for fine-grained control over commit filtering.</p>
 * 
 * <p>Usage example:</p>
 * <pre>
 * {@code
 * var navigator = GitHubRepoNavigatorBuilder
 *     .forRepository("https://github.com/user/repo.git")
 *     .withPersonalAccessToken("ghp_xxxxxxxxxxxx")
 *     .localCloneDirectory("/tmp/repo")
 *     .fileFilters("*.java", "*.md")
 *     .branch("develop")
 *     .shallowClone(true)
 *     .build();
 * }
 * </pre>
 * 
 * @since 1.0.0
 */
public class GitHubRepositoryBuilder {
  private final RepositoryConfig.Builder configBuilder;

  private GitHubRepositoryBuilder(String repositoryUrl) {
    this.configBuilder = RepositoryConfig.builder(repositoryUrl);
  }

  /**
   * Creates a new builder instance for the specified repository URL.
   * 
   * @param repositoryUrl the GitHub repository URL (HTTPS or SSH format)
   * @return a new {@link GitHubRepositoryBuilder} instance
   * @throws NullPointerException if repositoryUrl is null
   */
  public static GitHubRepositoryBuilder forRepository(String repositoryUrl) {
    return new GitHubRepositoryBuilder(repositoryUrl);
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
   * Configures file filters to limit commits based on file changes.
   * 
   * <p>Only commits that modify files matching these filters will be included
   * in the navigation. Supports glob patterns like "*.java" and "**\/*.md".</p>
   * 
   * @param filters variable number of file filter patterns
   * @return this builder instance for method chaining
   */
  public GitHubRepositoryBuilder fileFilters(String... filters) {
    configBuilder.fileFilters(Arrays.asList(filters));
    return this;
  }

  /**
   * Configures file filters to limit commits based on file changes.
   * 
   * <p>Only commits that modify files matching these filters will be included
   * in the navigation. Supports glob patterns like "*.java" and "**\/*.md".</p>
   * 
   * @param filters list of file filter patterns
   * @return this builder instance for method chaining
   */
  public GitHubRepositoryBuilder fileFilters(List<String> filters) {
    configBuilder.fileFilters(filters);
    return this;
  }

  /**
   * Specifies the branch to navigate through.
   * 
   * @param branch the branch name (defaults to "main" if not specified)
   * @return this builder instance for method chaining
   */
  public GitHubRepositoryBuilder branch(String branch) {
    configBuilder.branch(branch);
    return this;
  }

  /**
   * Specifies the commit hash to start navigation from.
   * 
   * @param commitHash the full or abbreviated commit hash
   * @return this builder instance for method chaining
   */
  public GitHubRepositoryBuilder startingCommit(String commitHash) {
    configBuilder.startingCommit(commitHash);
    return this;
  }

  /**
   * Configures whether to perform a shallow clone of the repository.
   * 
   * <p>Shallow clones only download the latest commit, reducing clone time
   * and disk usage, but limiting navigation to recent history.</p>
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
   * <p>Authentication is optional - if no authentication methods are called,
   * the navigator will attempt to access public repositories without authentication.</p>
   * 
   * @return a configured {@link GitHubRepository} instance
   * @throws IllegalStateException if required configuration parameters are missing
   */
  public GitHubRepository build() {
    var config = configBuilder.build();
    return GitHubRepositoryFactory.create(config);
  }
}