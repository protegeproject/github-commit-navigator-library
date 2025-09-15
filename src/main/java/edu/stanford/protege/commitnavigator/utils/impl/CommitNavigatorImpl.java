package edu.stanford.protege.commitnavigator.utils.impl;

import edu.stanford.protege.commitnavigator.config.CommitNavigatorConfig;
import edu.stanford.protege.commitnavigator.exceptions.RepositoryException;
import edu.stanford.protege.commitnavigator.model.CommitMetadata;
import edu.stanford.protege.commitnavigator.utils.CommitNavigator;
import edu.stanford.protege.commitnavigator.utils.FileChangeDetector;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link CommitNavigator} that provides sequential navigation through Git
 * repository commits with optional file filtering.
 *
 * <p>This implementation maintains a filtered list of commits based on the configured file filters
 * and provides bidirectional navigation through the commit history. It supports both navigation
 * with and without working directory checkout operations.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Lazy initialization of commit list on first access
 *   <li>File-based filtering of commits using glob patterns
 *   <li>Bidirectional navigation (next/previous)
 *   <li>Optional checkout operations during navigation
 *   <li>Configurable starting commit position
 * </ul>
 *
 * <p>The navigator maintains an internal index to track the current position in the filtered commit
 * list, allowing for efficient navigation operations.
 *
 * @since 1.0.0
 */
public class CommitNavigatorImpl implements CommitNavigator {
  private static final Logger logger = LoggerFactory.getLogger(CommitNavigatorImpl.class);

  private final Repository repository;
  private final CommitNavigatorConfig navigatorConfig;
  private final FileChangeDetector fileChangeDetector;

  private List<RevCommit> filteredCommits;
  private int currentIndex;
  private boolean initialized = false;

  /**
   * Constructs a new CommitNavigatorImpl with the specified dependencies.
   *
   * <p>The navigator is initialized with a current index of -1, indicating no current commit. The
   * actual commit list is built lazily on first access to improve performance.
   *
   * @param repository the Git repository to navigate
   * @param navigatorConfig the navigation configuration containing file filters and starting commit
   * @param fileChangeDetector the service for detecting file changes in commits
   * @throws NullPointerException if any parameter is null
   */
  @Inject
  public CommitNavigatorImpl(
      Repository repository,
      CommitNavigatorConfig navigatorConfig,
      FileChangeDetector fileChangeDetector) {
    this.repository = Objects.requireNonNull(repository, "Repository cannot be null");
    this.navigatorConfig =
        Objects.requireNonNull(navigatorConfig, "NavigatorConfig cannot be null");
    this.fileChangeDetector =
        Objects.requireNonNull(fileChangeDetector, "FileChangeDetector cannot be null");
    this.currentIndex = -1;
  }

  /**
   * Moves to the next commit in the filtered commit history.
   *
   * <p>This method advances the internal index and returns the metadata of the next commit. If no
   * next commit is available, returns null.
   *
   * @return the {@link CommitMetadata} of the next commit, or null if no next commit exists
   * @throws RepositoryException if an error occurs during navigation or repository access
   */
  @Override
  public CommitMetadata next() throws RepositoryException {
    ensureInitialized();

    if (!hasNext()) {
      return null;
    }

    currentIndex++;
    var commit = filteredCommits.get(currentIndex);
    logger.debug("Navigated to next commit: {}", commit.getName());
    return createCommitMetadata(commit);
  }

  /**
   * Moves to the previous commit in the filtered commit history.
   *
   * <p>This method decrements the internal index and returns the metadata of the previous commit.
   * If no previous commit is available, returns null.
   *
   * @return the {@link CommitMetadata} of the previous commit, or null if no previous commit exists
   * @throws RepositoryException if an error occurs during navigation or repository access
   */
  @Override
  public CommitMetadata previous() throws RepositoryException {
    ensureInitialized();

    if (!hasPrevious()) {
      return null;
    }

    currentIndex--;
    var commit = filteredCommits.get(currentIndex);
    logger.debug("Navigated to previous commit: {}", commit.getName());
    return createCommitMetadata(commit);
  }

  /**
   * Moves to the next commit and checks out the working directory to that commit.
   *
   * <p>This method combines navigation and checkout operations. It first moves to the next commit,
   * then checks out the working directory to match that commit's state. If no next commit is
   * available, no checkout is performed.
   *
   * @return the {@link CommitMetadata} of the next commit, or null if no next commit exists
   * @throws RepositoryException if an error occurs during navigation, checkout, or repository
   *     access
   */
  @Override
  public CommitMetadata nextAndCheckout() throws RepositoryException {
    var commitMetadata = next();
    if (commitMetadata != null) {
      checkout(commitMetadata.getCommitHash());
    }
    return commitMetadata;
  }

  /**
   * Moves to the previous commit and checks out the working directory to that commit.
   *
   * <p>This method combines navigation and checkout operations. It first moves to the previous
   * commit, then checks out the working directory to match that commit's state. If no previous
   * commit is available, no checkout is performed.
   *
   * @return the {@link CommitMetadata} of the previous commit, or null if no previous commit exists
   * @throws RepositoryException if an error occurs during navigation, checkout, or repository
   *     access
   */
  @Override
  public CommitMetadata previousAndCheckout() throws RepositoryException {
    var commitMetadata = previous();
    if (commitMetadata != null) {
      checkout(commitMetadata.getCommitHash());
    }
    return commitMetadata;
  }

  /**
   * Checks if there is a next commit available for navigation.
   *
   * <p>This method determines if the current index position allows for forward navigation in the
   * filtered commit list.
   *
   * @return true if there is a next commit available, false otherwise
   * @throws RepositoryException if an error occurs during repository access or initialization
   */
  @Override
  public boolean hasNext() throws RepositoryException {
    ensureInitialized();
    return currentIndex < filteredCommits.size() - 1;
  }

  /**
   * Checks if there is a previous commit available for navigation.
   *
   * <p>This method determines if the current index position allows for backward navigation in the
   * filtered commit list.
   *
   * @return true if there is a previous commit available, false otherwise
   * @throws RepositoryException if an error occurs during repository access or initialization
   */
  @Override
  public boolean hasPrevious() throws RepositoryException {
    ensureInitialized();
    return currentIndex > 0;
  }

  /**
   * Returns the total number of commits available in the navigation sequence.
   *
   * @return the total commit count
   * @throws RepositoryException if an error occurs while getting for the total commit count
   */
  @Override
  public long getCommitCount() throws RepositoryException {
    ensureInitialized();
    return filteredCommits.size();
  }

  /**
   * Returns the metadata of the current commit without changing the navigation position.
   *
   * <p>This method provides access to the current commit's metadata without affecting the
   * navigation state. If no current commit is set (index is -1 or invalid), returns null.
   *
   * @return the {@link CommitMetadata} of the current commit, or null if no current commit is set
   * @throws RepositoryException if an error occurs during repository access or initialization
   */
  @Override
  public CommitMetadata getCurrentCommit() throws RepositoryException {
    ensureInitialized();

    if (currentIndex >= 0 && currentIndex < filteredCommits.size()) {
      return createCommitMetadata(filteredCommits.get(currentIndex));
    }
    return null;
  }

  /**
   * Resolves a relative file path to its absolute path within the repository.
   *
   * <p>This method takes a file path relative to the repository root and returns the absolute path
   * on the local filesystem. The path can use forward slashes regardless of the operating system.
   *
   * @param relativePath the relative file path within the repository (e.g.,
   *     "src/main/java/Main.java")
   * @return the absolute {@link Path} to the file on the local filesystem
   * @throws NullPointerException if relativePath is null
   */
  @Override
  public Path resolveFilePath(String relativePath) {
    Objects.requireNonNull(relativePath, "Relative path cannot be null");
    var localDirectory = repository.getWorkTree().toPath();
    return localDirectory.resolve(Paths.get(relativePath));
  }

  /**
   * Resets the navigator to its initial state.
   *
   * <p>This method clears the filtered commit list, resets the current index to -1, and marks the
   * navigator as uninitialized. The next navigation operation will trigger re-initialization and
   * rebuild the filtered commit list.
   *
   * @throws RepositoryException if an error occurs during the reset operation
   */
  @Override
  public void reset() throws RepositoryException {
    logger.debug("Resetting commit navigator");
    initialized = false;
    filteredCommits = null;
    currentIndex = -1;
  }

  private void ensureInitialized() throws RepositoryException {
    if (!initialized) {
      initialize();
    }
  }

  private void initialize() throws RepositoryException {
    logger.debug("Initializing commit navigator");

    try {
      filteredCommits = buildFilteredCommitList();

      if (navigatorConfig.getStartingCommit().isPresent()) {
        var startingCommit = navigatorConfig.getStartingCommit().get();
        currentIndex = findCommitIndex(startingCommit);
        if (currentIndex == -1) {
          logger.warn(
              "No commit found for starting commit {} that includes the file in the filter",
              startingCommit);
          logger.warn("Starting at the latest commit instead.");
          currentIndex = filteredCommits.size() - 1;
        }
      } else {
        currentIndex = filteredCommits.size() - 1;
      }

      initialized = true;
      logger.debug(
          "Navigator initialized with {} filtered commits, starting at index {}",
          filteredCommits.size(),
          currentIndex);

    } catch (Exception e) {
      throw new RepositoryException("Failed to initialize commit navigator", e);
    }
  }

  private List<RevCommit> buildFilteredCommitList() throws RepositoryException {
    logger.debug("Building filtered commit list");

    try (RevWalk revWalk = new RevWalk(repository)) {
      revWalk.markStart(revWalk.parseCommit(repository.resolve("HEAD")));

      var commits = new ArrayList<RevCommit>();

      for (RevCommit commit : revWalk) {
        if (shouldIncludeCommit(commit)) {
          commits.add(commit);
        }
      }

      commits.sort((c1, c2) -> c1.getCommitTime() - c2.getCommitTime());

      return commits;

    } catch (IOException e) {
      throw new RepositoryException("Failed to build commit list", e);
    }
  }

  private boolean shouldIncludeCommit(RevCommit commit) throws RepositoryException {
    var fileFilters = navigatorConfig.getFileFilters();

    if (fileFilters.isEmpty()) {
      return true;
    }

    return fileChangeDetector.hasFileChanges(repository, commit, fileFilters.get());
  }

  private int findCommitIndex(String commitHash) {
    for (int i = 0; i < filteredCommits.size(); i++) {
      if (filteredCommits.get(i).getName().equals(commitHash)) {
        return i;
      }
    }
    return -1;
  }

  private void checkout(String commitHash) throws RepositoryException {
    logger.debug("Checking out commit: {}", commitHash);

    try {
      var git = new Git(repository);
      var checkout = git.checkout();
      checkout.setName(commitHash);
      checkout.call();

      logger.debug("Successfully checked out commit: {}", commitHash);

    } catch (GitAPIException e) {
      throw new RepositoryException("Failed to checkout commit: " + commitHash, e);
    }
  }

  private CommitMetadata createCommitMetadata(RevCommit commit) {
    var commitHash = commit.getName();
    var committerUsername = commit.getCommitterIdent().getName();
    var committerEmail = commit.getCommitterIdent().getEmailAddress();
    var commitDate = commit.getCommitterIdent().getWhen().toInstant();
    var commitMessage = commit.getFullMessage();

    return CommitMetadata.create(
        commitHash, committerUsername, committerEmail, commitDate, commitMessage);
  }
}
