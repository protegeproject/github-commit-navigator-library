package com.github.navigator.cli;

import com.github.navigator.GitHubRepoNavigator;
import com.github.navigator.GitHubRepoNavigatorBuilder;
import com.github.navigator.services.CommitNavigator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@Command(name = "github-navigator", 
         description = "Navigate GitHub repository commits programmatically",
         mixinStandardHelpOptions = true,
         version = "1.0.0")
public class GitHubNavigatorCli implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(GitHubNavigatorCli.class);
    
    @Parameters(index = "0", description = "The GitHub repository URL (e.g., https://github.com/user/repo.git)")
    private String repositoryUrl;
    
    @Option(names = {"-b", "--branch"}, description = "The branch to use (default: ${DEFAULT-VALUE})", defaultValue = "main")
    private String branch;
    
    @Option(names = {"-d", "--clone-directory"}, description = "Local directory path where the repository will be cloned (defaults to temp directory)")
    private String cloneDirectory;
    
    @Option(names = {"-f", "--file-filter"}, description = "File filter pattern (e.g., '*.java' or '*.java,*.md' for multiple filters)")
    private String fileFilter;
    
    @Option(names = {"-t", "--token"}, description = "GitHub personal access token for private repositories")
    private String token;
    
    public static void main(String[] args) {
        int exitCode = new CommandLine(new GitHubNavigatorCli()).execute(args);
        System.exit(exitCode);
    }
    
    @Override
    public Integer call() throws Exception {
        System.out.println("GitHub Navigator CLI");
        
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
            
            return 0; // Success
            
        } catch (Exception e) {
            logger.error("Error during navigation", e);
            System.err.println("Error: " + e.getMessage());
            return 1; // Error
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