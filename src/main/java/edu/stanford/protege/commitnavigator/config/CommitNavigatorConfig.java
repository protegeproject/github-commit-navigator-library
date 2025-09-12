package edu.stanford.protege.commitnavigator.config;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Configuration class for commit navigator parameters.
 *
 * <p>This class encapsulates configuration options specific to commit navigation, including file
 * filters for limiting which commits to include and the starting commit position. The configuration
 * is immutable and uses the builder pattern for construction.
 *
 * <p>This configuration is separate from repository-level configuration and focuses solely on
 * navigation behavior and filtering criteria.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * var navigatorConfig = CommitNavigatorConfig.builder()
 *     .fileFilters(List.of("*.java", "*.md"))
 *     .startingCommit("abc123def")
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public class CommitNavigatorConfig {
  private final List<String> fileFilters;
  private final String startingCommit;

  private CommitNavigatorConfig(Builder builder) {
    this.fileFilters = builder.fileFilters;
    this.startingCommit = builder.startingCommit;
  }

  /**
   * Returns the file filter patterns for limiting commit navigation.
   *
   * <p>These patterns are used to filter commits based on which files were modified. Only commits
   * that modified files matching these patterns will be included in the navigation sequence.
   *
   * @return the list of file filter patterns, or null if no filters are specified
   */
  public Optional<List<String>> getFileFilters() {
    return Optional.ofNullable(fileFilters);
  }

  /**
   * Returns the starting commit hash for navigation.
   *
   * <p>When specified, navigation will begin from this commit rather than the latest commit in the
   * repository. The commit must exist in the repository's commit history.
   *
   * @return an {@link Optional} containing the starting commit hash, or empty if not specified
   */
  public Optional<String> getStartingCommit() {
    return Optional.ofNullable(startingCommit);
  }

  public static CommitNavigatorConfig getDefault() {
    return builder().build();
  }

  /**
   * Creates a new builder instance for constructing CommitNavigatorConfig.
   *
   * @return a new {@link Builder} instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder class for constructing {@link CommitNavigatorConfig} instances. */
  public static class Builder {

    @Nullable private ImmutableList<String> fileFilters;

    @Nullable private String startingCommit;

    private Builder() {}

    /**
     * Sets the file filters for limiting commit navigation.
     *
     * <p>File filters can be exact file paths or glob patterns. Supports exact paths, wildcard
     * patterns, and directory patterns.
     *
     * @param fileFilters the list of file filter patterns
     * @return this builder instance for method chaining
     */
    public Builder fileFilters(@Nonnull List<String> fileFilters) {
      this.fileFilters = ImmutableList.copyOf(fileFilters);
      return this;
    }

    /**
     * Sets the file filters for limiting commit navigation using variable arguments.
     *
     * <p>File filters can be exact file paths or glob patterns. Supports exact paths, wildcard
     * patterns, and directory patterns.
     *
     * <p>Convenient method for setting multiple file filters at once.
     *
     * @param fileFilters variable number of file filter patterns
     * @return this builder instance for method chaining
     */
    public Builder fileFilters(@Nonnull String... fileFilters) {
      return fileFilters(Arrays.asList(fileFilters));
    }

    /**
     * Sets the starting commit hash for navigation.
     *
     * <p>When specified, navigation will begin from this commit rather than the latest commit. The
     * commit hash should be a valid Git commit SHA.
     *
     * @param startingCommit the commit hash to start navigation from
     * @return this builder instance for method chaining
     */
    public Builder startingCommit(String startingCommit) {
      this.startingCommit = startingCommit;
      return this;
    }

    /**
     * Builds a {@link CommitNavigatorConfig} instance with the configured parameters.
     *
     * @return a new {@link CommitNavigatorConfig} instance
     */
    public CommitNavigatorConfig build() {
      return new CommitNavigatorConfig(this);
    }
  }
}
