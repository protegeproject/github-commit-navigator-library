package edu.stanford.protege.commitnavigator.config;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class CommitNavigatorConfigTest {

  @Test
  void testBuilderPattern() {
    var startingCommit = "abc123";
    var fileFilters = Arrays.asList("*.java", "*.md");

    var config =
        CommitNavigatorConfig.builder()
            .fileFilters(fileFilters)
            .startingCommit(startingCommit)
            .build();

    assertTrue(config.getFileFilters().isPresent());
    assertEquals(fileFilters, config.getFileFilters().get());
    assertTrue(config.getStartingCommit().isPresent());
    assertEquals(startingCommit, config.getStartingCommit().get());
  }

  @Test
  void testDefaultValues() {
    var config = CommitNavigatorConfig.builder().build();

    assertFalse(config.getFileFilters().isPresent());
    assertFalse(config.getStartingCommit().isPresent());
  }

  @Test
  void testFileFiltersOnly() {
    var fileFilters = Arrays.asList("*.java", "*.xml", "pom.xml");

    var config = CommitNavigatorConfig.builder().fileFilters(fileFilters).build();

    assertTrue(config.getFileFilters().isPresent());
    assertEquals(fileFilters, config.getFileFilters().get());
    assertFalse(config.getStartingCommit().isPresent());
  }

  @Test
  void testStartingCommitOnly() {
    var startingCommit = "def456";

    var config = CommitNavigatorConfig.builder().startingCommit(startingCommit).build();

    assertFalse(config.getFileFilters().isPresent());
    assertTrue(config.getStartingCommit().isPresent());
    assertEquals(startingCommit, config.getStartingCommit().get());
  }

  @Test
  void testNullFileFilters() {
    var config = CommitNavigatorConfig.builder().build();

    assertFalse(config.getFileFilters().isPresent());
  }

  @Test
  void testEmptyFileFilters() {
    var config = CommitNavigatorConfig.builder().fileFilters(List.of()).build();

    assertTrue(config.getFileFilters().isPresent());
    assertTrue(config.getFileFilters().get().isEmpty());
  }

  @Test
  void testFileFiltersImmutability() {
    var originalFilters = Arrays.asList("*.java", "*.md");
    var config = CommitNavigatorConfig.builder().fileFilters(originalFilters).build();

    var retrievedFilters = config.getFileFilters();
    assertTrue(retrievedFilters.isPresent());
    assertEquals(originalFilters, retrievedFilters.get());

    // Ensure returned list is immutable
    assertThrows(UnsupportedOperationException.class, () -> retrievedFilters.get().add("*.xml"));
  }

  @Test
  void testFileFiltersVarArgs() {
    var config = CommitNavigatorConfig.builder().fileFilters("*.java", "*.md", "pom.xml").build();

    var expectedFilters = Arrays.asList("*.java", "*.md", "pom.xml");
    assertTrue(config.getFileFilters().isPresent());
    assertEquals(expectedFilters, config.getFileFilters().get());
  }

  @Test
  void testFileFiltersVarArgsEmpty() {
    var config = CommitNavigatorConfig.builder().fileFilters().build();

    assertTrue(config.getFileFilters().isPresent());
    assertTrue(config.getFileFilters().get().isEmpty());
  }

  @Test
  void testFileFiltersVarArgsSingle() {
    var config = CommitNavigatorConfig.builder().fileFilters("src/main/java/Main.java").build();

    var expectedFilters = Arrays.asList("src/main/java/Main.java");
    assertTrue(config.getFileFilters().isPresent());
    assertEquals(expectedFilters, config.getFileFilters().get());
  }

  @Test
  void testFileFiltersVarArgsNull() {
    var config = CommitNavigatorConfig.builder().build();

    assertFalse(config.getFileFilters().isPresent());
  }
}
