package com.github.navigator.config;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

  public String getRepositoryUrl() {
    return repositoryUrl;
  }

  public Path getLocalCloneDirectory() {
    return localCloneDirectory;
  }

  public List<String> getFileFilters() {
    return fileFilters;
  }

  public String getBranch() {
    return branch;
  }

  public Optional<String> getStartingCommit() {
    return Optional.ofNullable(startingCommit);
  }

  public boolean isShallowClone() {
    return shallowClone;
  }

  public Optional<AuthenticationConfig> getAuthConfig() {
    return Optional.ofNullable(authConfig);
  }

  public static Builder builder(String repositoryUrl) {
    return new Builder(repositoryUrl);
  }

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

    public Builder localCloneDirectory(Path localCloneDirectory) {
      this.localCloneDirectory = localCloneDirectory;
      return this;
    }

    public Builder fileFilters(List<String> fileFilters) {
      this.fileFilters = fileFilters;
      return this;
    }

    public Builder branch(String branch) {
      this.branch = branch;
      return this;
    }

    public Builder startingCommit(String startingCommit) {
      this.startingCommit = startingCommit;
      return this;
    }

    public Builder shallowClone(boolean shallowClone) {
      this.shallowClone = shallowClone;
      return this;
    }

    public Builder authConfig(AuthenticationConfig authConfig) {
      this.authConfig = authConfig;
      return this;
    }

    public NavigatorConfig build() {
      return new NavigatorConfig(this);
    }
  }
}