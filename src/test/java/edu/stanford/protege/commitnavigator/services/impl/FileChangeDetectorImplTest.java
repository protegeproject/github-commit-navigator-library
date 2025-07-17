package edu.stanford.protege.commitnavigator.services.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class FileChangeDetectorImplTest {

  private FileChangeDetectorImpl fileChangeDetector;

  @BeforeEach
  void setUp() {
    fileChangeDetector = new FileChangeDetectorImpl();
  }

  @Test
  void testHasFileChanges_NoFilters() {
    assertTrue(fileChangeDetector.matchesAnyFilter("test.java", null));
    assertTrue(fileChangeDetector.matchesAnyFilter("test.java", Collections.emptyList()));
  }

  @Test
  void testMatchesFilter_ExactMatch() {
    var detector = new FileChangeDetectorImpl();
        
    assertTrue(detector.matchesFilter("src/main/java/Test.java", "src/main/java/Test.java"));
    assertTrue(detector.matchesFilter("src/main/java/Test.java", "Test.java"));
    assertFalse(detector.matchesFilter("src/main/java/Test.java", "Other.java"));
  }

  @Test
  void testMatchesFilter_GlobPattern() {
    var detector = new FileChangeDetectorImpl();
    
    assertTrue(detector.matchesFilter("Test.java", "*.java"));
    assertTrue(detector.matchesFilter("Test.java", "Test.*"));
    assertFalse(detector.matchesFilter("Test.java", "*.txt"));
    
    assertTrue(detector.matchesFilter("src/main/java/Test.java", "**/*.java"));
    assertTrue(detector.matchesFilter("src/main/java/Test.java", "src/**/*.java"));
    assertFalse(detector.matchesFilter("src/main/java/Test.java", "**/*.txt"));
  }

  @Test
  void testMatchesAnyFilter() {
    var detector = new FileChangeDetectorImpl();
    var filters = Arrays.asList("*.java", "*.md", "pom.xml");
    
    assertTrue(detector.matchesAnyFilter("Test.java", filters));
    assertTrue(detector.matchesAnyFilter("README.md", filters));
    assertTrue(detector.matchesAnyFilter("pom.xml", filters));
    assertFalse(detector.matchesAnyFilter("test.txt", filters));
  }

  @Test
  void testGetFilePath_DifferentChangeTypes() {
    var detector = new FileChangeDetectorImpl();
    
  }
}