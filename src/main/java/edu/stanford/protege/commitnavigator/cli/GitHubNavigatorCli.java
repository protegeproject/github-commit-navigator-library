package edu.stanford.protege.commitnavigator.cli;

import edu.stanford.protege.commitnavigator.GitHubRepositoryBuilderFactory;
import edu.stanford.protege.commitnavigator.config.CommitNavigatorConfig;
import edu.stanford.protege.commitnavigator.model.RepositoryCoordinates;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Command-line interface for the GitHub repository commit navigator.
 *
 * <p>This class provides a command-line tool for analyzing GitHub repository commits with support
 * for authentication, file filtering, and branch selection. It uses the PicoCLI library for
 * argument parsing and command execution.
 *
 * <p>Usage examples:
 *
 * <pre>{@code
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
 * }</pre>
 *
 * @since 1.0.0
 */
@Command(
    name = "github-navigator",
    description = "Navigate GitHub repository commits programmatically",
    mixinStandardHelpOptions = true,
    version = "1.0.0")
public class GitHubNavigatorCli implements Callable<Integer> {
  private static final Logger logger = LoggerFactory.getLogger(GitHubNavigatorCli.class);

  @Parameters(
      index = "0",
      description = "The GitHub repository URL (e.g., https://github.com/user/repo.git)")
  private String repositoryUrl;

  @Option(
      names = {"-b", "--branch"},
      description = "The branch to use (default: ${DEFAULT-VALUE})",
      defaultValue = "main")
  private String branch;

  @Option(
      names = {"-d", "--clone-directory"},
      description =
          "Local directory path where the repository will be cloned (defaults to system temp directory)")
  private String cloneDirectory;

  @Option(
      names = {"-f", "--file-filter"},
      description = "File filter pattern (e.g., '*.java' or '*.java,*.md' for multiple filters)")
  private String fileFilter;

  @Option(
      names = {"-t", "--token"},
      description = "GitHub personal access token for private repositories")
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
    try {
      // Extract repository coordinate from URL
      var coordinate = RepositoryCoordinates.createFromUrl(repositoryUrl, branch);

      // Create repositoryBuilder using factory pattern
      var repositoryBuilder = GitHubRepositoryBuilderFactory.create(coordinate);

      if (cloneDirectory != null) {
        // Configure additional options
        repositoryBuilder.localCloneDirectory(cloneDirectory);
      }

      var repository = repositoryBuilder.build();

      repository.initialize();

      // Create navigator configuration with file filters if provided
      var navigatorConfigBuilder = CommitNavigatorConfig.builder();
      if (fileFilter != null && !fileFilter.trim().isEmpty()) {
        var filters = fileFilter.split(",");
        for (int i = 0; i < filters.length; i++) {
          filters[i] = filters[i].trim();
        }
        navigatorConfigBuilder.fileFilters(filters); // Using varargs method
      }
      var navigatorConfig = navigatorConfigBuilder.build();

      var commitNavigator = repository.getCommitNavigator(navigatorConfig);

      while (commitNavigator.hasParent()) {
        var commit = commitNavigator.fetchParent();
        System.out.println(
            "commit "
                + commit.getCommitHash()
                + " by "
                + commit.getCommitterUsername()
                + "("
                + commit.getCommitterEmail()
                + ")"
                + " on "
                + commit.getCommitDate()
                + "\n"
                + "   "
                + commit.getCommitMessage().trim()
                + "\n");
      }

      repository.close();

      return 0; // Success

    } catch (Exception e) {
      logger.error("Error during navigation", e);
      System.err.println("Error: " + e.getMessage());
      return 1; // Error
    }
  }
}
