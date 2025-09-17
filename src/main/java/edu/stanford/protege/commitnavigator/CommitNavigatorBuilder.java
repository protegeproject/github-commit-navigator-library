package edu.stanford.protege.commitnavigator;

import edu.stanford.protege.commitnavigator.exceptions.RepositoryException;
import edu.stanford.protege.commitnavigator.utils.CommitNavigator;
import edu.stanford.protege.commitnavigator.utils.FileChangeAnalyzer;
import edu.stanford.protege.commitnavigator.utils.impl.CommitNavigatorImpl;
import edu.stanford.protege.commitnavigator.utils.impl.FileChangeAnalyzerImpl;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builder class for creating {@link CommitNavigator} instances with flexible configuration options.
 *
 * <p>This builder provides a fluent API for configuring commit navigation parameters including file
 * filtering and starting commit position. The builder handles repository opening, project history
 * construction, and filtering to create properly configured commit navigator instances.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * var navigator = CommitNavigatorBuilder.forWorkingDirectory("/path/to/repo")
 *     .fileFilters("*.java", "*.md")
 *     .startingCommit("abc123def")
 *     .build();
 *
 * // Navigate through filtered commits
 * while (navigator.hasParent()) {
 *     var commit = navigator.checkoutParent();
 *     System.out.println("Processing: " + commit.getCommitHash());
 * }
 * }</pre>
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Mandatory working directory specification
 *   <li>Optional file filtering using glob patterns
 *   <li>Optional starting commit position
 * </ul>
 *
 * @since 1.0.0
 */
public class CommitNavigatorBuilder {
  private static final Logger logger = LoggerFactory.getLogger(CommitNavigatorBuilder.class);

  /** The working directory containing the Git repository. */
  private final Path workingDirectory;

  /** Optional file filters for limiting commit inclusion. */
  @Nullable private List<String> fileFilters;

  /** Optional starting commit hash for navigation. */
  @Nullable private String startingCommit;

  /**
   * Creates a new CommitNavigatorBuilder for the specified working directory.
   *
   * @param workingDirectory the path to the Git repository working directory
   * @throws NullPointerException if workingDirectory is null
   */
  private CommitNavigatorBuilder(@Nonnull Path workingDirectory) {
    this.workingDirectory =
        Objects.requireNonNull(workingDirectory, "Working directory cannot be null");
  }

  /**
   * Creates a new CommitNavigatorBuilder for the specified working directory.
   *
   * <p>This is the primary entry point for creating commit navigators. The working directory must
   * contain a valid Git repository (.git directory).
   *
   * @param workingDirectory the string path to the Git repository working directory
   * @return a new {@link CommitNavigatorBuilder} instance
   * @throws NullPointerException if workingDirectory is null
   */
  public static CommitNavigatorBuilder forWorkingDirectory(@Nonnull String workingDirectory) {
    return forWorkingDirectory(Paths.get(workingDirectory));
  }

  /**
   * Creates a new CommitNavigatorBuilder for the specified working directory.
   *
   * <p>This is the primary entry point for creating commit navigators. The working directory must
   * contain a valid Git repository (.git directory).
   *
   * @param workingDirectory the path to the Git repository working directory
   * @return a new {@link CommitNavigatorBuilder} instance
   * @throws NullPointerException if workingDirectory is null
   */
  public static CommitNavigatorBuilder forWorkingDirectory(@Nonnull Path workingDirectory) {
    return new CommitNavigatorBuilder(workingDirectory);
  }

  /**
   * Sets the file filters for limiting commit navigation.
   *
   * <p>File filters can be exact file paths or glob patterns. Only commits that modified files
   * matching these patterns will be included in the navigation sequence. Supports exact paths,
   * wildcard patterns, and directory patterns.
   *
   * <p>Examples:
   *
   * <ul>
   *   <li>Exact paths: "src/main/java/Example.java"
   *   <li>Glob patterns: "*.java", "**&#47;*.md", "src/**&#47;*.xml"
   * </ul>
   *
   * @param fileFilters the list of file filter patterns
   * @return this builder instance for method chaining
   * @throws NullPointerException if fileFilters is null
   */
  public CommitNavigatorBuilder fileFilters(@Nonnull List<String> fileFilters) {
    this.fileFilters = Objects.requireNonNull(fileFilters, "File filters cannot be null");
    return this;
  }

  /**
   * Sets the file filters for limiting commit navigation using variable arguments.
   *
   * <p>File filters can be exact file paths or glob patterns. This is a convenience method for
   * setting multiple file filters at once without creating a List.
   *
   * <p>Examples:
   *
   * <ul>
   *   <li>Exact paths: "src/main/java/Example.java"
   *   <li>Glob patterns: "*.java", "**&#47;*.md", "src/**&#47;*.xml"
   * </ul>
   *
   * @param fileFilters variable number of file filter patterns
   * @return this builder instance for method chaining
   * @throws NullPointerException if fileFilters is null
   */
  public CommitNavigatorBuilder fileFilters(@Nonnull String... fileFilters) {
    Objects.requireNonNull(fileFilters, "File filters cannot be null");
    return fileFilters(Arrays.asList(fileFilters));
  }

  /**
   * Sets the starting commit hash for navigation.
   *
   * <p>When specified, navigation will begin from this commit rather than the latest commit (HEAD).
   * The commit hash should be a valid Git commit SHA that exists in the repository's commit
   * history.
   *
   * @param commitHash the commit hash to start navigation from
   * @return this builder instance for method chaining
   * @throws NullPointerException if commitHash is null
   */
  public CommitNavigatorBuilder startingCommit(@Nonnull String commitHash) {
    this.startingCommit = Objects.requireNonNull(commitHash, "Starting commit cannot be null");
    return this;
  }

  /**
   * Builds a {@link CommitNavigator} instance with the configured parameters.
   *
   * <p>This method performs the following operations:
   *
   * <ul>
   *   <li>Opens the Git repository from the working directory
   *   <li>Collects all commits in chronological order (HEAD first)
   *   <li>Filters commits based on file filters if specified
   *   <li>Finds the starting commit index if specified
   *   <li>Creates and returns a configured CommitNavigator instance
   * </ul>
   *
   * @return a configured {@link CommitNavigator} instance ready for use
   * @throws RepositoryException if the repository cannot be opened, commits cannot be collected, or
   *     the starting commit is not found
   */
  public CommitNavigator build() throws RepositoryException {
    logger.info("Preparing commit navigator for directory: {}", workingDirectory);

    try {
      // Open the Git repository
      var repository = openRepository();
      var git = new Git(repository);

      // Collect all commits
      var filteredCommits = collectFilteredCommits(repository);
      logger.info("Collected {} total commits", filteredCommits.size());

      // Find starting index
      var startingIndex = findStartingIndex(filteredCommits);
      logger.info("Using starting index: {}", startingIndex);

      // Create and return the navigator
      var navigator = new CommitNavigatorImpl(git, filteredCommits, startingIndex);
      logger.info("Successfully created commit navigator");
      return navigator;

    } catch (IOException e) {
      throw new RepositoryException("Failed to build commit navigator", e);
    }
  }

  /**
   * Opens the Git repository from the working directory.
   *
   * @return the opened Repository instance
   * @throws IOException if the repository cannot be opened
   * @throws RepositoryException if no Git repository is found at the working directory
   */
  private Repository openRepository() throws IOException, RepositoryException {
    var builder = new FileRepositoryBuilder();
    var repository =
        builder.setGitDir(workingDirectory.resolve(".git").toFile()).readEnvironment().build();

    if (repository.getObjectDatabase() == null) {
      throw new RepositoryException("No Git repository found at: " + workingDirectory);
    }
    return repository;
  }

  /**
   * Collects all commits from the repository in chronological order (HEAD first, oldest last).
   *
   * @param repository the Git repository to collect commits from
   * @return a list of all commits in chronological order
   * @throws IOException if commit collection fails
   * @throws RepositoryException if the repository has no commits
   */
  private List<RevCommit> collectFilteredCommits(Repository repository)
      throws IOException, RepositoryException {
    logger.debug("Collecting relevant commits from repository");

    var fileChangeAnalyzer = new FileChangeAnalyzerImpl();

    try (var revWalk = new RevWalk(repository)) {
      revWalk.markStart(revWalk.parseCommit(repository.resolve("HEAD")));

      var commits = new ArrayList<RevCommit>();
      for (RevCommit commit : revWalk) {
        if (shouldIncludeCommit(repository, commit, fileChangeAnalyzer)) {
          commits.add(commit);
        }
      }
      return commits;

    } catch (IOException e) {
      throw new RepositoryException("Failed to build commit list", e);
    }
  }

  private boolean shouldIncludeCommit(
      Repository repository, RevCommit commit, FileChangeAnalyzer fileChangeAnalyzer)
      throws RepositoryException {
    if (fileFilters == null || fileFilters.isEmpty()) {
      return true;
    }
    return fileChangeAnalyzer.hasFileChanges(repository, commit, fileFilters);
  }

  /**
   * Finds the starting index in the filtered commit list.
   *
   * @param filteredCommits the filtered list of commits
   * @return the starting index, or 0 if no starting commit is specified
   * @throws RepositoryException if the starting commit is specified but not found
   */
  private int findStartingIndex(List<RevCommit> filteredCommits) throws RepositoryException {
    if (startingCommit == null) {
      logger.debug("No starting commit hash specified, using HEAD (index 0)");
      return 0;
    }

    logger.debug("Looking for starting commit hash: {}", startingCommit);

    for (int i = 0; i < filteredCommits.size(); i++) {
      if (filteredCommits.get(i).getName().equals(startingCommit)) {
        logger.debug("Found starting commit hash at index: {}", i);
        return i;
      }
    }
    logger.debug("Starting commit hash not found, fall back to using HEAD (index 0)");
    return 0;
  }
}
