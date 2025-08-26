package edu.stanford.protege.commitnavigator.config;

import static org.junit.jupiter.api.Assertions.*;

import edu.stanford.protege.commitnavigator.model.RepositoryCoordinate;
import java.nio.file.Paths;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class RepositoryConfigTest {

  @Test
  void testBuilderPattern() {
    var startingCommit = "abc123";
    var coordinate = RepositoryCoordinate.create("example", "repo", "develop");

    var config =
        RepositoryConfig.builder(coordinate)
            .localCloneDirectory(Paths.get("/tmp/test"))
            .fileFilters(Arrays.asList("*.java", "*.md"))
            .startingCommit(startingCommit)
            .shallowClone(true)
            .build();

    assertEquals("https://github.com/example/repo.git", config.getRepositoryUrl());
    assertEquals(Paths.get("/tmp/test"), config.getLocalCloneDirectory());
    assertEquals(Arrays.asList("*.java", "*.md"), config.getFileFilters());
    assertEquals("develop", config.getBranch());
    assertTrue(config.getStartingCommit().isPresent());
    assertEquals(startingCommit, config.getStartingCommit().get());
    assertTrue(config.isShallowClone());
  }

  @Test
  void testDefaultValues() {
    var coordinate = RepositoryCoordinate.create("example", "repo");

    var config = RepositoryConfig.builder(coordinate).build();

    assertEquals("https://github.com/example/repo.git", config.getRepositoryUrl());
    assertNotNull(config.getLocalCloneDirectory()); // Default directory is set
    assertNull(config.getFileFilters());
    assertEquals("main", config.getBranch());
    assertFalse(config.getStartingCommit().isPresent());
    assertFalse(config.isShallowClone());
    assertFalse(config.getAuthConfig().isPresent());
  }

  @Test
  void testAuthenticationConfig() {
    var coordinate = RepositoryCoordinate.create("example", "repo");
    var authConfig = AuthenticationConfig.personalAccessToken("token").build();

    var config = RepositoryConfig.builder(coordinate).authConfig(authConfig).build();

    assertTrue(config.getAuthConfig().isPresent());
    assertEquals(authConfig, config.getAuthConfig().get());
  }
}
