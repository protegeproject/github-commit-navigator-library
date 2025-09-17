package edu.stanford.protege.commitnavigator;

import static org.junit.jupiter.api.Assertions.*;

import edu.stanford.protege.commitnavigator.config.AuthenticationConfig;
import edu.stanford.protege.commitnavigator.model.RepositoryCoordinates;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class GitHubRepositoryBuilderTest {

  private static final RepositoryCoordinates TEST_COORDINATE =
      RepositoryCoordinates.create("example", "repo");

  @Test
  void testBuilderWithPersonalAccessToken() {
    var token = "test-token";

    var navigator =
        GitHubRepositoryBuilderFactory.create(TEST_COORDINATE)
            .withPersonalAccessToken(token)
            .build();

    assertNotNull(navigator);
    var config = navigator.getConfig();
    assertEquals("https://github.com/example/repo", config.getRepositoryUrl());
    assertEquals("https://github.com/example/repo.git", config.getCloneUrl());
    assertTrue(config.getAuthConfig().isPresent());
    assertEquals(
        AuthenticationConfig.AuthenticationType.PERSONAL_ACCESS_TOKEN,
        config.getAuthConfig().get().getType());
    assertEquals(token, config.getAuthConfig().get().getToken());
  }

  @Test
  void testBuilderWithOAuthToken() {
    var token = "oauth-token";

    var navigator =
        GitHubRepositoryBuilderFactory.create(TEST_COORDINATE).withOAuthToken(token).build();

    assertNotNull(navigator);
    var config = navigator.getConfig();
    assertTrue(config.getAuthConfig().isPresent());
    assertEquals(
        AuthenticationConfig.AuthenticationType.OAUTH, config.getAuthConfig().get().getType());
    assertEquals(token, config.getAuthConfig().get().getToken());
  }

  @Test
  void testBuilderWithUsernamePassword() {
    var username = "testuser";
    var password = "testpass";

    var navigator =
        GitHubRepositoryBuilderFactory.create(TEST_COORDINATE)
            .withUsernamePassword(username, password)
            .build();

    assertNotNull(navigator);
    var config = navigator.getConfig();
    assertTrue(config.getAuthConfig().isPresent());
    assertEquals(
        AuthenticationConfig.AuthenticationType.USERNAME_PASSWORD,
        config.getAuthConfig().get().getType());
    assertEquals(username, config.getAuthConfig().get().getUsername());
    assertEquals(password, config.getAuthConfig().get().getPassword());
  }

  @Test
  void testBuilderWithAllOptions() {
    var token = "test-token";
    var localPath = "/tmp/test-repo";
    var branch = "develop";
    var coordinate = RepositoryCoordinates.create("example", "repo", branch);

    var navigator =
        GitHubRepositoryBuilderFactory.create(coordinate)
            .withPersonalAccessToken(token)
            .localWorkingDirectory(localPath)
            .shallowClone(true)
            .build();

    assertNotNull(navigator);
    var config = navigator.getConfig();
    assertEquals("https://github.com/example/repo", config.getRepositoryUrl());
    assertEquals("https://github.com/example/repo.git", config.getCloneUrl());
    assertEquals(Paths.get(localPath), config.getLocalWorkingDirectory());
    assertEquals(branch, config.getBranch());
    assertTrue(config.isShallowClone());
  }

  @Test
  void testBuilderWithoutAuthentication() {
    var publicRepoCoordinate = RepositoryCoordinates.create("example", "public-repo");

    var navigator = GitHubRepositoryBuilderFactory.create(publicRepoCoordinate).build();

    assertNotNull(navigator);
    var config = navigator.getConfig();
    assertEquals("https://github.com/example/public-repo", config.getRepositoryUrl());
    assertEquals("https://github.com/example/public-repo.git", config.getCloneUrl());
    assertFalse(config.getAuthConfig().isPresent());
    assertEquals("main", config.getBranch());
  }
}
