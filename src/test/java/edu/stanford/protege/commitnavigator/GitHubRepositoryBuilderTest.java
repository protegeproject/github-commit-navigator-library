package edu.stanford.protege.commitnavigator;

import edu.stanford.protege.commitnavigator.config.AuthenticationConfig;
import edu.stanford.protege.commitnavigator.config.RepositoryConfig;
import edu.stanford.protege.commitnavigator.model.RepositoryCoordinate;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class GitHubRepositoryBuilderTest {

  private static final RepositoryCoordinate TEST_COORDINATE = 
      RepositoryCoordinate.create("example", "repo");

  @Test
  void testBuilderWithPersonalAccessToken() {
    String token = "test-token";

    GitHubRepository navigator = GitHubRepositoryBuilderFactory
      .create(TEST_COORDINATE)
      .withPersonalAccessToken(token)
      .build();

    assertNotNull(navigator);
    RepositoryConfig config = navigator.getConfig();
    assertEquals("https://github.com/example/repo.git", config.getRepositoryUrl());
    assertTrue(config.getAuthConfig().isPresent());
    assertEquals(AuthenticationConfig.AuthenticationType.PERSONAL_ACCESS_TOKEN,
      config.getAuthConfig().get().getType());
    assertEquals(token, config.getAuthConfig().get().getToken());
  }

  @Test
  void testBuilderWithOAuthToken() {
    String token = "oauth-token";

    GitHubRepository navigator = GitHubRepositoryBuilderFactory
      .create(TEST_COORDINATE)
      .withOAuthToken(token)
      .build();

    assertNotNull(navigator);
    RepositoryConfig config = navigator.getConfig();
    assertTrue(config.getAuthConfig().isPresent());
    assertEquals(AuthenticationConfig.AuthenticationType.OAUTH,
      config.getAuthConfig().get().getType());
    assertEquals(token, config.getAuthConfig().get().getToken());
  }

  @Test
  void testBuilderWithUsernamePassword() {
    String username = "testuser";
    String password = "testpass";

    GitHubRepository navigator = GitHubRepositoryBuilderFactory
      .create(TEST_COORDINATE)
      .withUsernamePassword(username, password)
      .build();

    assertNotNull(navigator);
    RepositoryConfig config = navigator.getConfig();
    assertTrue(config.getAuthConfig().isPresent());
    assertEquals(AuthenticationConfig.AuthenticationType.USERNAME_PASSWORD,
      config.getAuthConfig().get().getType());
    assertEquals(username, config.getAuthConfig().get().getUsername());
    assertEquals(password, config.getAuthConfig().get().getPassword());
  }

  @Test
  void testBuilderWithAllOptions() {
    String token = "test-token";
    String localPath = "/tmp/test-repo";
    String branch = "develop";
    String startingCommit = "abc123";
    RepositoryCoordinate coordinate = RepositoryCoordinate.create("example", "repo", branch);

    GitHubRepository navigator = GitHubRepositoryBuilderFactory
      .create(coordinate)
      .withPersonalAccessToken(token)
      .localCloneDirectory(localPath)
      .fileFilters("*.java", "*.md")
      .startingCommit(startingCommit)
      .shallowClone(true)
      .build();

    assertNotNull(navigator);
    RepositoryConfig config = navigator.getConfig();
    assertEquals("https://github.com/example/repo.git", config.getRepositoryUrl());
    assertEquals(Paths.get(localPath), config.getLocalCloneDirectory());
    assertEquals(Arrays.asList("*.java", "*.md"), config.getFileFilters());
    assertEquals(branch, config.getBranch());
    assertTrue(config.getStartingCommit().isPresent());
    assertEquals(startingCommit, config.getStartingCommit().get());
    assertTrue(config.isShallowClone());
  }

  @Test
  void testBuilderWithoutAuthentication() {
    RepositoryCoordinate publicRepoCoordinate = RepositoryCoordinate.create("example", "public-repo");

    GitHubRepository navigator = GitHubRepositoryBuilderFactory
      .create(publicRepoCoordinate)
      .build();

    assertNotNull(navigator);
    RepositoryConfig config = navigator.getConfig();
    assertEquals("https://github.com/example/public-repo.git", config.getRepositoryUrl());
    assertFalse(config.getAuthConfig().isPresent());
    assertEquals("main", config.getBranch());
  }
}