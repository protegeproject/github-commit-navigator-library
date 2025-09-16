package edu.stanford.protege.commitnavigator.config;

import static org.junit.jupiter.api.Assertions.*;

import edu.stanford.protege.commitnavigator.model.RepositoryCoordinates;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class RepositoryConfigTest {

  @Test
  void testBuilderPattern() {
    var coordinate = RepositoryCoordinates.create("example", "repo", "develop");

    var config =
        RepositoryConfig.builder(coordinate)
            .localCloneDirectory(Paths.get("/tmp/test"))
            .shallowClone(true)
            .build();

    assertEquals("https://github.com/example/repo", config.getRepositoryUrl());
    assertEquals("https://github.com/example/repo.git", config.getCloneUrl());
    assertEquals(Paths.get("/tmp/test"), config.getLocalCloneDirectory());
    assertEquals("develop", config.getBranch());
    assertTrue(config.isShallowClone());
  }

  @Test
  void testDefaultValues() {
    var coordinate = RepositoryCoordinates.create("example", "repo");

    var config = RepositoryConfig.builder(coordinate).build();

    assertEquals("https://github.com/example/repo", config.getRepositoryUrl());
    assertEquals("https://github.com/example/repo.git", config.getCloneUrl());
    assertNotNull(config.getLocalCloneDirectory()); // Default directory is set
    assertEquals("main", config.getBranch());
    assertFalse(config.isShallowClone());
    assertFalse(config.getAuthConfig().isPresent());
  }

  @Test
  void testAuthenticationConfig() {
    var coordinate = RepositoryCoordinates.create("example", "repo");
    var authConfig = AuthenticationConfig.personalAccessToken("token").build();

    var config = RepositoryConfig.builder(coordinate).authConfig(authConfig).build();

    assertTrue(config.getAuthConfig().isPresent());
    assertEquals(authConfig, config.getAuthConfig().get());
  }
}
