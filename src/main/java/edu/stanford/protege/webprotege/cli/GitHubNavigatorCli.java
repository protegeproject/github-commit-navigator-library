package edu.stanford.protege.webprotege.cli;

import edu.stanford.protege.webprotege.GitHubRepoNavigatorBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

/**
 * Command-line interface for the GitHub repository commit navigator.
 * 
 * <p>This class provides a command-line tool for analyzing GitHub repository commits
 * with support for authentication, file filtering, and branch selection. It uses
 * the PicoCLI library for argument parsing and command execution.</p>
 * 
 * <p>Usage examples:</p>
 * <pre>
 * {@code
 * // Basic usage
 * java -jar github-navigator.jar https://github.com/user/repo.git
 * 
 * // With authentication and filters
 * java -jar github-navigator.jar \
 *   --token ghp_xxxxxxxxxxxx \
 *   --file-filter "*.java,*.md" \
 *   --branch develop \
 *   --clone-directory /tmp/repo \
 *   https://github.com/user/repo.git
 * }
 * </pre>
 * 
 * @since 1.0.0
 */
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

  /**
   * Main entry point for the CLI application.
   * 
   * @param args command-line arguments
   */
  public static void main(String[] args) {
    var exitCode = new CommandLine(new GitHubNavigatorCli()).execute(args);
    System.exit(exitCode);
  }

  /**
   * Executes the CLI command to navigate through repository commits.
   * 
   * @return exit code (0 for success, 1 for error)
   * @throws Exception if an error occurs during execution
   */
  @Override
  public Integer call() throws Exception {
    // Set default clone directory to system temp directory if not provided
    if (cloneDirectory == null) {
      var repoName = extractRepositoryName(repositoryUrl);
      cloneDirectory = System.getProperty("java.io.tmpdir") + repoName;
    }
    System.out.println("Clone directory: " + cloneDirectory + "\n");

    try {
      var builder = GitHubRepoNavigatorBuilder
        .forRepository(repositoryUrl)
        .localCloneDirectory(cloneDirectory)
        .branch(branch);

      // Add file filters if provided, otherwise use default filters
      if (fileFilter != null && !fileFilter.trim().isEmpty()) {
        var filters = fileFilter.split(",");
        for (int i = 0; i < filters.length; i++) {
          filters[i] = filters[i].trim();
        }
        builder.fileFilters(filters);
      }

      // Add authentication only if token is provided
      if (token != null && !token.trim().isEmpty()) {
        builder.withPersonalAccessToken(token);
      }

      var navigator = builder.build();

      navigator.initialize();

      var commitNavigator = navigator.getCommitNavigator();

      while (commitNavigator.hasPrevious()) {
        var commit = commitNavigator.previous();
        System.out.println("commit " + commit.getCommitHash() +
          " by " + commit.getCommitterUsername() +
          " on " + commit.getCommitDate() + "\n" +
          "   " + commit.getCommitMessage().trim() + "\n");
      }

      navigator.close();

      return 0; // Success

    } catch (Exception e) {
      logger.error("Error during navigation", e);
      System.err.println("Error: " + e.getMessage());
      return 1; // Error
    }
  }

  /**
   * Extracts the repository name from a GitHub repository URL.
   * 
   * @param repositoryUrl the GitHub repository URL
   * @return the repository name without the .git suffix
   */
  private static String extractRepositoryName(String repositoryUrl) {
    var parts = repositoryUrl.split("/");
    var repoName = parts[parts.length - 1];
    if (repoName.endsWith(".git")) {
      repoName = repoName.substring(0, repoName.length() - 4);
    }
    return repoName;
  }
}