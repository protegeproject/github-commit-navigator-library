package com.github.navigator.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class NavigatorConfigTest {

    @Test
    void testBuilderPattern() {
        String repositoryUrl = "https://github.com/example/repo.git";
        String branch = "develop";
        String startingCommit = "abc123";
        
        NavigatorConfig config = NavigatorConfig.builder(repositoryUrl)
            .localCloneDirectory(Paths.get("/tmp/test"))
            .fileFilters(Arrays.asList("*.java", "*.md"))
            .branch(branch)
            .startingCommit(startingCommit)
            .shallowClone(true)
            .build();
        
        assertEquals(repositoryUrl, config.getRepositoryUrl());
        assertEquals(Paths.get("/tmp/test"), config.getLocalCloneDirectory());
        assertEquals(Arrays.asList("*.java", "*.md"), config.getFileFilters());
        assertEquals(branch, config.getBranch());
        assertTrue(config.getStartingCommit().isPresent());
        assertEquals(startingCommit, config.getStartingCommit().get());
        assertTrue(config.isShallowClone());
    }

    @Test
    void testDefaultValues() {
        String repositoryUrl = "https://github.com/example/repo.git";
        
        NavigatorConfig config = NavigatorConfig.builder(repositoryUrl)
            .build();
        
        assertEquals(repositoryUrl, config.getRepositoryUrl());
        assertNull(config.getLocalCloneDirectory());
        assertNull(config.getFileFilters());
        assertEquals("main", config.getBranch());
        assertFalse(config.getStartingCommit().isPresent());
        assertFalse(config.isShallowClone());
        assertFalse(config.getAuthConfig().isPresent());
    }

    @Test
    void testAuthenticationConfig() {
        String repositoryUrl = "https://github.com/example/repo.git";
        AuthenticationConfig authConfig = AuthenticationConfig.personalAccessToken("token").build();
        
        NavigatorConfig config = NavigatorConfig.builder(repositoryUrl)
            .authConfig(authConfig)
            .build();
        
        assertTrue(config.getAuthConfig().isPresent());
        assertEquals(authConfig, config.getAuthConfig().get());
    }
}