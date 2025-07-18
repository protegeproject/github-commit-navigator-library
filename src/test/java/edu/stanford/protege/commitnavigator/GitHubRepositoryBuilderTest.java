package edu.stanford.protege.commitnavigator;

import edu.stanford.protege.commitnavigator.config.AuthenticationConfig;
import edu.stanford.protege.commitnavigator.config.RepositoryConfig;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class GitHubRepositoryBuilderTest {

  @Test
  void testBuilderWithPersonalAccessToken() {
    String repositoryUrl = "https://github.com/example/repo.git";
    String token = "test-token";

    GitHubRepository navigator = GitHubRepositoryBuilder
      .forRepository(repositoryUrl)
      .withPersonalAccessToken(token)
      .build();

    assertNotNull(navigator);
    RepositoryConfig config = navigator.getConfig();
    assertEquals(repositoryUrl, config.getRepositoryUrl());
    assertTrue(config.getAuthConfig().isPresent());
    assertEquals(AuthenticationConfig.AuthenticationType.PERSONAL_ACCESS_TOKEN,
      config.getAuthConfig().get().getType());
    assertEquals(token, config.getAuthConfig().get().getToken());
  }

  @Test
  void testBuilderWithOAuthToken() {
    String repositoryUrl = "https://github.com/example/repo.git";
    String token = "oauth-token";

    GitHubRepository navigator = GitHubRepositoryBuilder
      .forRepository(repositoryUrl)
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
    String repositoryUrl = "https://github.com/example/repo.git";
    String username = "testuser";
    String password = "testpass";

    GitHubRepository navigator = GitHubRepositoryBuilder
      .forRepository(repositoryUrl)
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
    String repositoryUrl = "https://github.com/example/repo.git";
    String token = "test-token";
    String localPath = "/tmp/test-repo";
    String branch = "develop";
    String startingCommit = "abc123";

    GitHubRepository navigator = GitHubRepositoryBuilder
      .forRepository(repositoryUrl)
      .withPersonalAccessToken(token)
      .localCloneDirectory(localPath)
      .fileFilters("*.java", "*.md")
      .branch(branch)
      .startingCommit(startingCommit)
      .shallowClone(true)
      .build();

    assertNotNull(navigator);
    RepositoryConfig config = navigator.getConfig();
    assertEquals(repositoryUrl, config.getRepositoryUrl());
    assertEquals(Paths.get(localPath), config.getLocalCloneDirectory());
    assertEquals(Arrays.asList("*.java", "*.md"), config.getFileFilters());
    assertEquals(branch, config.getBranch());
    assertTrue(config.getStartingCommit().isPresent());
    assertEquals(startingCommit, config.getStartingCommit().get());
    assertTrue(config.isShallowClone());
  }

  @Test
  void testBuilderWithoutAuthentication() {
    String repositoryUrl = "https://github.com/example/public-repo.git";

    GitHubRepository navigator = GitHubRepositoryBuilder
      .forRepository(repositoryUrl)
      .branch("main")
      .build();

    assertNotNull(navigator);
    RepositoryConfig config = navigator.getConfig();
    assertEquals(repositoryUrl, config.getRepositoryUrl());
    assertFalse(config.getAuthConfig().isPresent());
    assertEquals("main", config.getBranch());
  }
}