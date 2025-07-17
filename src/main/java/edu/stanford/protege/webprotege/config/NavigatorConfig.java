package edu.stanford.protege.webprotege.config;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Configuration class for GitHub repository navigation parameters.
 * 
 * <p>This class encapsulates all configuration options for repository navigation,
 * including repository details, file filters, authentication settings, and clone behavior.
 * The configuration is immutable and uses the builder pattern for construction.</p>
 * 
 * <p>Usage example:</p>
 * <pre>
 * {@code
 * var config = NavigatorConfig.builder("https://github.com/user/repo.git")
 *     .localCloneDirectory(Paths.get("/tmp/repo"))
 *     .fileFilters(List.of("*.java", "*.md"))
 *     .branch("develop")
 *     .shallowClone(true)
 *     .authConfig(authConfig)
 *     .build();
 * }
 * </pre>
 * 
 * @since 1.0.0
 */
public class NavigatorConfig {
  private final String repositoryUrl;
  private final Path localCloneDirectory;
  private final List<String> fileFilters;
  private final String branch;
  private final String startingCommit;
  private final boolean shallowClone;
  private final AuthenticationConfig authConfig;

  private NavigatorConfig(Builder builder) {
    this.repositoryUrl = Objects.requireNonNull(builder.repositoryUrl, "Repository URL cannot be null");
    this.localCloneDirectory = builder.localCloneDirectory;
    this.fileFilters = builder.fileFilters;
    this.branch = Objects.requireNonNull(builder.branch, "Branch cannot be null");
    this.startingCommit = builder.startingCommit;
    this.shallowClone = builder.shallowClone;
    this.authConfig = builder.authConfig;
  }

  /**
   * Returns the GitHub repository URL.
   * 
   * @return the repository URL (HTTPS or SSH format)
   */
  public String getRepositoryUrl() {
    return repositoryUrl;
  }

  /**
   * Returns the local directory path where the repository will be cloned.
   * 
   * @return the local clone directory path, or null if not specified
   */
  public Path getLocalCloneDirectory() {
    return localCloneDirectory;
  }

  /**
   * Returns the file filter patterns for limiting commit navigation.
   * 
   * @return the list of file filter patterns, or null if no filters are specified
   */
  public List<String> getFileFilters() {
    return fileFilters;
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
   * Returns the starting commit hash for navigation.
   * 
   * @return an {@link Optional} containing the starting commit hash, or empty if not specified
   */
  public Optional<String> getStartingCommit() {
    return Optional.ofNullable(startingCommit);
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
   * @return an {@link Optional} containing the authentication configuration, or empty if not specified
   */
  public Optional<AuthenticationConfig> getAuthConfig() {
    return Optional.ofNullable(authConfig);
  }

  /**
   * Creates a new builder instance for the specified repository URL.
   * 
   * @param repositoryUrl the GitHub repository URL
   * @return a new {@link Builder} instance
   * @throws NullPointerException if repositoryUrl is null
   */
  public static Builder builder(String repositoryUrl) {
    return new Builder(repositoryUrl);
  }

  /**
   * Builder class for constructing {@link NavigatorConfig} instances.
   */
  public static class Builder {

    private static final String DEFAULT_BRANCH_NAME = "main";
    private static final boolean DEFAULT_SHALLOW_CLONE = false;

    private final String repositoryUrl;
    private Path localCloneDirectory;
    private List<String> fileFilters;
    private String branch = DEFAULT_BRANCH_NAME;
    private String startingCommit;
    private boolean shallowClone = DEFAULT_SHALLOW_CLONE;
    private AuthenticationConfig authConfig;

    private Builder(String repositoryUrl) {
      this.repositoryUrl = Objects.requireNonNull(repositoryUrl, "Repository URL cannot be null");
    }

    /**
     * Sets the local directory where the repository will be cloned.
     * 
     * @param localCloneDirectory the local clone directory path
     * @return this builder instance for method chaining
     */
    public Builder localCloneDirectory(Path localCloneDirectory) {
      this.localCloneDirectory = localCloneDirectory;
      return this;
    }

    /**
     * Sets the file filters for limiting commit navigation.
     * 
     * @param fileFilters the list of file filter patterns
     * @return this builder instance for method chaining
     */
    public Builder fileFilters(List<String> fileFilters) {
      this.fileFilters = fileFilters;
      return this;
    }

    /**
     * Sets the branch to navigate through.
     * 
     * @param branch the branch name
     * @return this builder instance for method chaining
     */
    public Builder branch(String branch) {
      this.branch = branch;
      return this;
    }

    /**
     * Sets the starting commit hash for navigation.
     * 
     * @param startingCommit the commit hash to start navigation from
     * @return this builder instance for method chaining
     */
    public Builder startingCommit(String startingCommit) {
      this.startingCommit = startingCommit;
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
     * Builds a {@link NavigatorConfig} instance with the configured parameters.
     * 
     * @return a new {@link NavigatorConfig} instance
     */
    public NavigatorConfig build() {
      return new NavigatorConfig(this);
    }
  }
}