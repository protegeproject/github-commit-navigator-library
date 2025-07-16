package com.github.navigator.examples;

import com.github.navigator.GitHubRepoNavigator;
import com.github.navigator.GitHubRepoNavigatorBuilder;
import com.github.navigator.services.CommitNavigator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitHubNavigatorExample {
    private static final Logger logger = LoggerFactory.getLogger(GitHubNavigatorExample.class);
    
    public static void main(String[] args) {
        System.out.println("GitHub Navigator Example");
        if (args.length < 2) {
            System.err.println("Usage: java GitHubNavigatorExample <repository-url> <clone-directory> [personal-access-token]");
            System.err.println("  repository-url: The GitHub repository URL (e.g., https://github.com/user/repo.git)");
            System.err.println("  clone-directory: Local directory path where the repository will be cloned");
            System.err.println("  personal-access-token: Optional GitHub personal access token for private repositories");
            System.exit(1);
        }
        
        String repositoryUrl = args[0];
        String cloneDirectory = args[1];
        String token = args.length > 2 ? args[2] : null;
        
        try {
            GitHubRepoNavigatorBuilder builder = GitHubRepoNavigatorBuilder
                .forRepository(repositoryUrl)
                .localCloneDirectory(cloneDirectory)
                .branch("main")
                .fileFilters("*.java", "*.md");
            
            // Add authentication only if token is provided
            if (token != null && !token.trim().isEmpty()) {
                builder.withPersonalAccessToken(token);
                System.out.println("Using provided personal access token for authentication");
            } else {
                System.out.println("No authentication token provided - accessing public repository");
            }
            
            GitHubRepoNavigator navigator = builder.build();
            
            navigator.initialize();
            
            CommitNavigator commitNavigator = navigator.getCommitNavigator();
            
            logger.info("Starting commit navigation example");
            
            System.out.println("=== Forward Navigation (with checkout) ===");
            while (commitNavigator.hasNext()) {
                String commit = commitNavigator.next();
                System.out.println("Checked out commit: " + commit);
            }
            
            System.out.println("\n=== Backward Navigation (without checkout) ===");
            while (commitNavigator.hasPrevious()) {
                String commit = commitNavigator.previous();
                System.out.println("Previous commit: " + commit);
            }
            
            System.out.println("\nCurrent commit: " + commitNavigator.getCurrentCommit());
            
            navigator.close();
            
        } catch (Exception e) {
            logger.error("Error during navigation", e);
            System.exit(1);
        }
    }
}