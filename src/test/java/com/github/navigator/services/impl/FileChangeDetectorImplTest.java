package com.github.navigator.services.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileChangeDetectorImplTest {

    private FileChangeDetectorImpl fileChangeDetector;

    @BeforeEach
    void setUp() {
        fileChangeDetector = new FileChangeDetectorImpl();
    }

    @Test
    void testHasFileChanges_NoFilters() {
        // Test null and empty filter lists - should return true (include all)
        assertTrue(fileChangeDetector.matchesAnyFilter("test.java", null));
        assertTrue(fileChangeDetector.matchesAnyFilter("test.java", Collections.emptyList()));
    }

    @Test
    void testMatchesFilter_ExactMatch() {
        FileChangeDetectorImpl detector = new FileChangeDetectorImpl();
        
        assertTrue(detector.matchesFilter("src/main/java/Test.java", "src/main/java/Test.java"));
        assertTrue(detector.matchesFilter("src/main/java/Test.java", "Test.java"));
        assertFalse(detector.matchesFilter("src/main/java/Test.java", "Other.java"));
    }

    @Test
    void testMatchesFilter_GlobPattern() {
        FileChangeDetectorImpl detector = new FileChangeDetectorImpl();
        
        // Test simple glob patterns
        assertTrue(detector.matchesFilter("Test.java", "*.java"));
        assertTrue(detector.matchesFilter("Test.java", "Test.*"));
        assertFalse(detector.matchesFilter("Test.java", "*.txt"));
        
        // Test recursive glob patterns
        assertTrue(detector.matchesFilter("src/main/java/Test.java", "**/*.java"));
        assertTrue(detector.matchesFilter("src/main/java/Test.java", "src/**/*.java"));
        assertFalse(detector.matchesFilter("src/main/java/Test.java", "**/*.txt"));
    }

    @Test
    void testMatchesAnyFilter() {
        FileChangeDetectorImpl detector = new FileChangeDetectorImpl();
        List<String> filters = Arrays.asList("*.java", "*.md", "pom.xml");
        
        assertTrue(detector.matchesAnyFilter("Test.java", filters));
        assertTrue(detector.matchesAnyFilter("README.md", filters));
        assertTrue(detector.matchesAnyFilter("pom.xml", filters));
        assertFalse(detector.matchesAnyFilter("test.txt", filters));
    }

    @Test
    void testGetFilePath_DifferentChangeTypes() {
        FileChangeDetectorImpl detector = new FileChangeDetectorImpl();
        
        // Test with mocked DiffEntry would require more complex setup
        // This is a placeholder for the actual implementation
    }
}