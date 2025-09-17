package edu.stanford.protege.commitnavigator.config;

import edu.stanford.protege.commitnavigator.model.RepositoryCoordinates;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Configuration class for GitHub repository navigation parameters.
 *
 * <p>This class encapsulates repository-level configuration options including repository details,
 * authentication settings, and clone behavior. The configuration is immutable and uses the builder
 * pattern for construction.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * var config = RepositoryConfig.builder(repositoryCoordinates)
 *     .localWorkingDirectory(Paths.get("/tmp/repo"))
 *     .shallowClone(true)
 *     .authConfig(authConfig)
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public class RepositoryConfig {
  private final String repoUrl;
  private final String repoName;
  private final Path localWorkingDirectory;
  private final String branch;
  private final boolean shallowClone;
  private final AuthenticationConfig authConfig;

  private RepositoryConfig(Builder builder) {
    this.repoUrl = Objects.requireNonNull(builder.repoUrl, "Repository URL cannot be null");
    this.repoName = Objects.requireNonNull(builder.repoName, "Repository name cannot be null");
    this.branch = Objects.requireNonNull(builder.branch, "Branch cannot be null");
    this.localWorkingDirectory = builder.localWorkingDirectory;
    this.shallowClone = builder.shallowClone;
    this.authConfig = builder.authConfig;
  }

  /**
   * Returns the GitHub repository web URL.
   *
   * @return the repository URL
   */
  public String getRepositoryUrl() {
    return repoUrl;
  }

  /**
   * Returns the GitHub repository clone URL.
   *
   * @return the repository clone URL (HTTPS format)
   */
  public String getCloneUrl() {
    return repoUrl + ".git";
  }

  /**
   * Returns the name of the repository.
   *
   * @return the repository name.
   */
  public String getRepositoryName() {
    return repoName;
  }

  /**
   * Returns the local directory path where the repository will be cloned.
   *
   * @return the local working directory path, or null if not specified
   */
  public Path getLocalWorkingDirectory() {
    return localWorkingDirectory;
  }

  /**
   * Returns the branch name to navigate through.
   *
   * @return the branch name (defaults to "main")
   */
  public String getBranch() {
    return branch;
  }

  /**
   * Returns whether to perform a shallow clone of the repository.
   *
   * @return true if shallow clone is enabled, false otherwise
   */
  public boolean isShallowClone() {
    return shallowClone;
  }

  /**
   * Returns the authentication configuration.
   *
   * @return an {@link Optional} containing the authentication configuration, or empty if not
   *     specified
   */
  public Optional<AuthenticationConfig> getAuthConfig() {
    return Optional.ofNullable(authConfig);
  }

  /**
   * Creates a new builder instance for the specified repository URL.
   *
   * @param repositoryCoordinates the repository coordinate containing owner, repo, and branch
   * @return a new {@link Builder} instance
   * @throws NullPointerException if repositoryUrl is null
   */
  public static Builder builder(RepositoryCoordinates repositoryCoordinates) {
    return new Builder(
        repositoryCoordinates.repositoryUrl(),
        repositoryCoordinates.repositoryName(),
        repositoryCoordinates.branchName());
  }

  /** Builder class for constructing {@link RepositoryConfig} instances. */
  public static class Builder {

    private static final boolean DEFAULT_SHALLOW_CLONE = false;

    private final String repoUrl;
    private final String repoName;
    private final String branch;

    private Path localWorkingDirectory;
    private boolean shallowClone = DEFAULT_SHALLOW_CLONE;
    private AuthenticationConfig authConfig;

    private Builder(String repositoryUrl, String repoName, String branchName) {
      this.repoUrl = Objects.requireNonNull(repositoryUrl, "Repository URL cannot be null");
      this.repoName = Objects.requireNonNull(repoName, "Repository name cannot be null");
      this.branch = Objects.requireNonNull(branchName, "Branch name cannot be null");
      this.localWorkingDirectory =
          Path.of(System.getProperty("java.io.tmpdir"), repoName); // default clone directory
    }

    /**
     * Sets the local working directory where the repository will be cloned.
     *
     * @param workingDirectory the local working directory path
     * @return this builder instance for method chaining
     */
    public Builder localWorkingDirectory(Path workingDirectory) {
      this.localWorkingDirectory = workingDirectory;
      return this;
    }

    /**
     * Sets whether to perform a shallow clone of the repository.
     *
     * @param shallowClone true to enable shallow clone, false otherwise
     * @return this builder instance for method chaining
     */
    public Builder shallowClone(boolean shallowClone) {
      this.shallowClone = shallowClone;
      return this;
    }

    /**
     * Sets the authentication configuration.
     *
     * @param authConfig the authentication configuration
     * @return this builder instance for method chaining
     */
    public Builder authConfig(AuthenticationConfig authConfig) {
      this.authConfig = authConfig;
      return this;
    }

    /**
     * Builds a {@link RepositoryConfig} instance with the configured parameters.
     *
     * @return a new {@link RepositoryConfig} instance
     */
    public RepositoryConfig build() {
      return new RepositoryConfig(this);
    }
  }
}
