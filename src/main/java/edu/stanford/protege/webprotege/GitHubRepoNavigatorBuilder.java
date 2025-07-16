package edu.stanford.protege.webprotege;

import edu.stanford.protege.webprotege.config.AuthenticationConfig;
import edu.stanford.protege.webprotege.config.NavigatorConfig;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class GitHubRepoNavigatorBuilder {
  private final NavigatorConfig.Builder configBuilder;

  private GitHubRepoNavigatorBuilder(String repositoryUrl) {
    this.configBuilder = NavigatorConfig.builder(repositoryUrl);
  }

  public static GitHubRepoNavigatorBuilder forRepository(String repositoryUrl) {
    return new GitHubRepoNavigatorBuilder(repositoryUrl);
  }

  public GitHubRepoNavigatorBuilder withPersonalAccessToken(String token) {
    configBuilder.authConfig(AuthenticationConfig.personalAccessToken(token).build());
    return this;
  }

  public GitHubRepoNavigatorBuilder withOAuthToken(String token) {
    configBuilder.authConfig(AuthenticationConfig.oauth(token).build());
    return this;
  }

  public GitHubRepoNavigatorBuilder withUsernamePassword(String username, String password) {
    configBuilder.authConfig(AuthenticationConfig.usernamePassword(username, password).build());
    return this;
  }

  public GitHubRepoNavigatorBuilder withSshKey(String sshKeyPath) {
    configBuilder.authConfig(AuthenticationConfig.sshKey(sshKeyPath).build());
    return this;
  }

  public GitHubRepoNavigatorBuilder localCloneDirectory(String path) {
    configBuilder.localCloneDirectory(Paths.get(path));
    return this;
  }

  public GitHubRepoNavigatorBuilder localCloneDirectory(Path path) {
    configBuilder.localCloneDirectory(path);
    return this;
  }

  public GitHubRepoNavigatorBuilder fileFilters(String... filters) {
    configBuilder.fileFilters(Arrays.asList(filters));
    return this;
  }

  public GitHubRepoNavigatorBuilder fileFilters(List<String> filters) {
    configBuilder.fileFilters(filters);
    return this;
  }

  public GitHubRepoNavigatorBuilder branch(String branch) {
    configBuilder.branch(branch);
    return this;
  }

  public GitHubRepoNavigatorBuilder startingCommit(String commitHash) {
    configBuilder.startingCommit(commitHash);
    return this;
  }

  public GitHubRepoNavigatorBuilder shallowClone(boolean shallow) {
    configBuilder.shallowClone(shallow);
    return this;
  }

  /**
   * Builds a GitHubRepoNavigator instance.
   * Authentication is optional - if no authentication methods are called,
   * the navigator will attempt to access public repositories without authentication.
   *
   * @return A configured GitHubRepoNavigator instance
   */
  public GitHubRepoNavigator build() {
    var config = configBuilder.build();
    return GitHubRepoNavigatorFactory.create(config);
  }
}