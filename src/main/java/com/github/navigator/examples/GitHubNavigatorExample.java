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
        if (args.length < 1) {
            System.err.println("Usage: java GitHubNavigatorExample <repository-url> [branch] [clone-directory] [file-filter] [personal-access-token]");
            System.err.println("  repository-url: The GitHub repository URL (e.g., https://github.com/user/repo.git)");
            System.err.println("  branch: The branch to use (defaults to 'main' if not specified)");
            System.err.println("  clone-directory: Local directory path where the repository will be cloned (defaults to temp directory)");
            System.err.println("  file-filter: File filter pattern (e.g., '*.java' or '*.java,*.md' for multiple filters)");
            System.err.println("  personal-access-token: Optional GitHub personal access token for private repositories");
            System.exit(1);
        }
        
        String repositoryUrl = args[0];
        String branch = args.length > 1 ? args[1] : "main";
        String cloneDirectory = args.length > 2 ? args[2] : null;
        String fileFilter = args.length > 3 ? args[3] : null;
        String token = args.length > 4 ? args[4] : null;
        
        // Set default clone directory to system temp directory if not provided
        if (cloneDirectory == null) {
            String repoName = extractRepositoryName(repositoryUrl);
            cloneDirectory = System.getProperty("java.io.tmpdir") + "/" + repoName;
            System.out.println("Clone directory not specified, using: " + cloneDirectory);
        } else {
            System.out.println("Using specified clone directory: " + cloneDirectory);
        }
        
        try {
            GitHubRepoNavigatorBuilder builder = GitHubRepoNavigatorBuilder
                .forRepository(repositoryUrl)
                .localCloneDirectory(cloneDirectory)
                .branch(branch);
            
            // Add file filters if provided, otherwise use default filters
            if (fileFilter != null && !fileFilter.trim().isEmpty()) {
                String[] filters = fileFilter.split(",");
                for (int i = 0; i < filters.length; i++) {
                    filters[i] = filters[i].trim();
                }
                builder.fileFilters(filters);
                System.out.println("Using file filters: " + String.join(", ", filters));
            } else {
                System.out.println("No file filter specified");
            }
            
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
    
    private static String extractRepositoryName(String repositoryUrl) {
        String[] parts = repositoryUrl.split("/");
        String repoName = parts[parts.length - 1];
        if (repoName.endsWith(".git")) {
            repoName = repoName.substring(0, repoName.length() - 4);
        }
        return repoName;
    }
}