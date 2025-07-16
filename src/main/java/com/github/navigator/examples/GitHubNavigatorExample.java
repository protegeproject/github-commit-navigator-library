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
            System.err.println("Usage: java GitHubNavigatorExample <repository-url> <personal-access-token>");
            System.exit(1);
        }
        
        String repositoryUrl = args[0];
        String token = args[1];
        
        try {
            GitHubRepoNavigator navigator = GitHubRepoNavigatorBuilder
                .forRepository(repositoryUrl)
                .withPersonalAccessToken(token)
                .branch("main")
                .fileFilters("*.java", "*.md")
                .localCloneDirectory("/tmp/github-navigator-example")
                .build();
            
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