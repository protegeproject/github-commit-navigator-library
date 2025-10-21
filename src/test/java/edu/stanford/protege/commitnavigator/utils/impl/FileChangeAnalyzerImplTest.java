package edu.stanford.protege.commitnavigator.utils.impl;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FileChangeAnalyzerImplTest {

  private FileChangeAnalyzerImpl fileChangeDetector;

  @BeforeEach
  void setUp() {
    fileChangeDetector = new FileChangeAnalyzerImpl();
  }

  @Test
  void testHasFileChanges_NoFilters() {
    assertTrue(fileChangeDetector.matchesAnyFilter("test.java", null));
    assertTrue(fileChangeDetector.matchesAnyFilter("test.java", Collections.emptyList()));
  }

  @Test
  void testMatchesFilter_ExactMatch() {
    var detector = new FileChangeAnalyzerImpl();

    assertTrue(detector.matchesFilter("src/main/java/Test.java", "src/main/java/Test.java"));
    assertTrue(detector.matchesFilter("src/main/java/Test.java", "Test.java"));
    assertFalse(detector.matchesFilter("src/main/java/Test.java", "Other.java"));
  }

  @Test
  void testMatchesFilter_GlobPattern() {
    var detector = new FileChangeAnalyzerImpl();

    assertTrue(detector.matchesFilter("Test.java", "*.java"));
    assertTrue(detector.matchesFilter("Test.java", "Test.*"));
    assertFalse(detector.matchesFilter("Test.java", "*.txt"));

    assertTrue(detector.matchesFilter("src/main/java/Test.java", "**/*.java"));
    assertTrue(detector.matchesFilter("src/main/java/Test.java", "src/**/*.java"));
    assertFalse(detector.matchesFilter("src/main/java/Test.java", "**/*.txt"));
  }

  @Test
  void testMatchesAnyFilter() {
    var detector = new FileChangeAnalyzerImpl();
    var filters = Arrays.asList("*.java", "*.md", "pom.xml");

    assertTrue(detector.matchesAnyFilter("Test.java", filters));
    assertTrue(detector.matchesAnyFilter("README.md", filters));
    assertTrue(detector.matchesAnyFilter("pom.xml", filters));
    assertFalse(detector.matchesAnyFilter("test.txt", filters));
  }

  @Test
  void testMatchesFilter_GlobPatternWithDoubleStarIncludesRootDirectory() {
    var detector = new FileChangeAnalyzerImpl();

    // Test that **/*.owl matches files in root directory
    assertTrue(detector.matchesFilter("grocery.owl", "**/*.owl"));
    assertTrue(detector.matchesFilter("ontology.owl", "**/*.owl"));

    // Test that **/*.owl still matches files in subdirectories
    assertTrue(detector.matchesFilter("src/main/resources/grocery.owl", "**/*.owl"));
    assertTrue(detector.matchesFilter("data/ontologies/pizza.owl", "**/*.owl"));

    // Test that **/*.owl matches files deep in subdirectories
    assertTrue(detector.matchesFilter("dir1/grocery.owl", "**/*.owl"));
    assertTrue(detector.matchesFilter("dir1/dir2/grocery.owl", "**/*.owl"));
    assertTrue(detector.matchesFilter("dir1/dir2/dir3/grocery.owl", "**/*.owl"));
    assertTrue(detector.matchesFilter("a/b/c/d/e/f/ontology.owl", "**/*.owl"));

    // Test that it doesn't match wrong extensions
    assertFalse(detector.matchesFilter("grocery.txt", "**/*.owl"));
    assertFalse(detector.matchesFilter("src/data.xml", "**/*.owl"));
    assertFalse(detector.matchesFilter("dir1/dir2/dir3/data.txt", "**/*.owl"));
  }

  @Test
  void testMatchesFilter_GlobPatternWithDoubleStarForMultipleExtensions() {
    var detector = new FileChangeAnalyzerImpl();

    // Test **/*.java matches root and subdirectory files
    assertTrue(detector.matchesFilter("Main.java", "**/*.java"));
    assertTrue(detector.matchesFilter("src/Main.java", "**/*.java"));
    assertTrue(detector.matchesFilter("src/main/java/Main.java", "**/*.java"));

    // Test **/*.md matches root and subdirectory files
    assertTrue(detector.matchesFilter("README.md", "**/*.md"));
    assertTrue(detector.matchesFilter("docs/README.md", "**/*.md"));
  }

  @Test
  void testMatchesFilter_RegularGlobPatternsStillWork() {
    var detector = new FileChangeAnalyzerImpl();

    // Test simple glob patterns without **/ prefix
    assertTrue(detector.matchesFilter("Test.java", "*.java"));
    assertTrue(detector.matchesFilter("README.md", "*.md"));
    assertFalse(detector.matchesFilter("src/Test.java", "*.java"));

    // Test with specific directory prefix
    assertTrue(detector.matchesFilter("src/main/java/Test.java", "src/**/*.java"));
    assertFalse(detector.matchesFilter("Test.java", "src/**/*.java"));
  }

  @Test
  void testMatchesFilter_SingleStarOnlyMatchesRootDirectory() {
    var detector = new FileChangeAnalyzerImpl();

    // Pattern *.owl should ONLY match files in root directory
    assertTrue(detector.matchesFilter("grocery.owl", "*.owl"));
    assertTrue(detector.matchesFilter("ontology.owl", "*.owl"));

    // Should NOT match files in any subdirectories
    assertFalse(detector.matchesFilter("src/grocery.owl", "*.owl"));
    assertFalse(detector.matchesFilter("dir1/grocery.owl", "*.owl"));
    assertFalse(detector.matchesFilter("dir1/dir2/grocery.owl", "*.owl"));
    assertFalse(detector.matchesFilter("dir1/dir2/dir3/grocery.owl", "*.owl"));

    // Should NOT match wrong extensions even in root
    assertFalse(detector.matchesFilter("grocery.txt", "*.owl"));
  }

  @Test
  void testMatchesFilter_DirectoryPrefixOnlyMatchesDirectFilesInThatDirectory() {
    var detector = new FileChangeAnalyzerImpl();

    // Pattern src/*.owl should ONLY match files directly in src directory
    assertTrue(detector.matchesFilter("src/grocery.owl", "src/*.owl"));
    assertTrue(detector.matchesFilter("src/ontology.owl", "src/*.owl"));

    // Should NOT match files in root
    assertFalse(detector.matchesFilter("grocery.owl", "src/*.owl"));

    // Should NOT match files in subdirectories of src
    assertFalse(detector.matchesFilter("src/main/grocery.owl", "src/*.owl"));
    assertFalse(detector.matchesFilter("src/main/resources/grocery.owl", "src/*.owl"));
    assertFalse(detector.matchesFilter("src/data/ontology.owl", "src/*.owl"));

    // Should NOT match files in other directories
    assertFalse(detector.matchesFilter("data/grocery.owl", "src/*.owl"));
    assertFalse(detector.matchesFilter("other/grocery.owl", "src/*.owl"));

    // Should NOT match wrong extensions even in src
    assertFalse(detector.matchesFilter("src/grocery.txt", "src/*.owl"));
  }

  @Test
  void testMatchesFilter_NestedDirectoryPrefixOnlyMatchesDirectFilesInThatPath() {
    var detector = new FileChangeAnalyzerImpl();

    // Pattern src/main/*.java should ONLY match files directly in src/main
    assertTrue(detector.matchesFilter("src/main/App.java", "src/main/*.java"));
    assertTrue(detector.matchesFilter("src/main/Main.java", "src/main/*.java"));

    // Should NOT match files in root or parent directories
    assertFalse(detector.matchesFilter("App.java", "src/main/*.java"));
    assertFalse(detector.matchesFilter("src/App.java", "src/main/*.java"));

    // Should NOT match files in subdirectories
    assertFalse(detector.matchesFilter("src/main/java/App.java", "src/main/*.java"));
    assertFalse(detector.matchesFilter("src/main/resources/App.java", "src/main/*.java"));
  }
}
