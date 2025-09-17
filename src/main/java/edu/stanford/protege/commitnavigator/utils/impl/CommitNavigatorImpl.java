package edu.stanford.protege.commitnavigator.utils.impl;

import com.google.common.collect.Lists;
import edu.stanford.protege.commitnavigator.exceptions.RepositoryException;
import edu.stanford.protege.commitnavigator.model.CommitMetadata;
import edu.stanford.protege.commitnavigator.utils.CommitNavigator;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link CommitNavigator} that provides sequential navigation through a
 * list of Git repository commits in chronological order (HEAD first, oldest last).
 *
 * <p>This implementation operates on a provided list of commits and provides bidirectional
 * navigation through the commit history. All navigation operations include working directory
 * checkout of a target commit.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Bidirectional navigation (child/parent) through commit history
 *   <li>Checkout operations during navigation
 *   <li>Configurable starting commit position
 *   <li>Path resolution utilities for repository files
 *   <li>Reset functionality to return to initial state
 * </ul>
 *
 * <p>The navigator maintains an internal index to track the current position in the commit list,
 * allowing for efficient navigation operations. The commit list is expected to be in chronological
 * order with HEAD first and oldest commit last.
 *
 * @since 1.0.0
 */
public class CommitNavigatorImpl implements CommitNavigator {
  private static final Logger logger = LoggerFactory.getLogger(CommitNavigatorImpl.class);

  /** The Git repository instance used for operations. */
  private final Git git;

  /** The list of commits in chronological order (HEAD first, oldest last). */
  private final List<RevCommit> projectHistory;

  /** The initial index position in the commit history. */
  private final int startingIndex;

  /** The current index position in the commit history. */
  private int currentIndex;

  /**
   * Creates a new CommitNavigatorImpl starting from the HEAD commit (index 0).
   *
   * @param git the Git repository instance, cannot be null
   * @param projectHistory the list of commits in chronological order (HEAD first), cannot be null
   */
  public CommitNavigatorImpl(@Nonnull Git git, @Nonnull List<RevCommit> projectHistory) {
    this(git, projectHistory, 0); // Starts from the HEAD by default
  }

  /**
   * Creates a new CommitNavigatorImpl with a specific starting position.
   *
   * @param git the Git repository instance, cannot be null
   * @param projectHistory the list of commits in chronological order (HEAD first), cannot be null
   * @param startingIndex the initial position in the commit history (0 = HEAD)
   * @throws NullPointerException if git or projectHistory is null
   */
  public CommitNavigatorImpl(
      @Nonnull Git git, @Nonnull List<RevCommit> projectHistory, int startingIndex) {
    this.git = Objects.requireNonNull(git, "git cannot be null");
    this.projectHistory = Objects.requireNonNull(projectHistory, "projectHistory cannot be null");
    this.startingIndex = startingIndex;
    this.currentIndex = startingIndex;
  }

  /**
   * Moves to the child commit and checks out the working directory to that commit.
   *
   * <p>This method combines navigation and checkout operations. It first moves to the child commit,
   * then checks out the working directory to match that commit's state. If no child commit is
   * available, no checkout is performed.
   *
   * @return the {@link CommitMetadata} of the child commit, or null if no child commit exists
   * @throws RepositoryException if an error occurs during navigation, checkout, or repository
   *     access
   */
  @Override
  public CommitMetadata checkoutChild() throws RepositoryException {
    try {
      // Move to the previous commit index and get the commit object
      currentIndex--;
      var commit = projectHistory.get(currentIndex);
      logger.debug("Navigated to child commit: {}", commit.getName());

      // Checkout that commit
      checkout(commit.getName());

      return createCommitMetadata(commit);
    } catch (IndexOutOfBoundsException e) {
      throw new RepositoryException(
          "Traversal stopped: reached HEAD, no further child commits available", e);
    }
  }

  /**
   * Moves to the parent commit and checks out the working directory to that commit.
   *
   * <p>This method combines navigation and checkout operations. It first moves to the parent
   * commit, then checks out the working directory to match that commit's state. If no parent commit
   * is available, no checkout is performed.
   *
   * @return the {@link CommitMetadata} of the parent commit, or null if no parent commit exists
   * @throws RepositoryException if an error occurs during navigation, checkout, or repository
   *     access
   */
  @Override
  public CommitMetadata checkoutParent() throws RepositoryException {
    try {
      // Move to the next commit index and get the commit object
      currentIndex++;
      var commit = projectHistory.get(currentIndex);
      logger.debug("Navigated to parent commit: {}", commit.getName());

      // Checkout that commit
      checkout(commit.getName());

      return createCommitMetadata(commit);
    } catch (IndexOutOfBoundsException e) {
      throw new RepositoryException(
          "Traversal stopped: reached initial commit, no further parent commits available", e);
    }
  }

  /**
   * Checks if there is a child commit available for navigation.
   *
   * <p>This method determines if the current index position allows for forward navigation in the
   * filtered commit list.
   *
   * @return true if there is a child commit available, false otherwise
   * @throws RepositoryException if an error occurs during repository access or initialization
   */
  @Override
  public boolean hasChild() throws RepositoryException {
    return currentIndex > 0;
  }

  /**
   * Checks if there is a parent commit available for navigation.
   *
   * <p>This method determines if the current index position allows for backward navigation in the
   * filtered commit list.
   *
   * @return true if there is a parent commit available, false otherwise
   * @throws RepositoryException if an error occurs during repository access or initialization
   */
  @Override
  public boolean hasParent() throws RepositoryException {
    return currentIndex < projectHistory.size() - 1;
  }

  /**
   * Returns the total number of commits available in the navigation sequence.
   *
   * @return the total commit count
   * @throws RepositoryException if an error occurs while getting for the total commit count
   */
  @Override
  public long getCommitCount() throws RepositoryException {
    return projectHistory.size();
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
    if (currentIndex >= 0 && currentIndex < projectHistory.size()) {
      return createCommitMetadata(projectHistory.get(currentIndex));
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
    var localDirectory = git.getRepository().getWorkTree().toPath();
    return localDirectory.resolve(Paths.get(relativePath));
  }

  /**
   * Resets the navigator to its initial state.
   *
   * <p>This method resets the current index to the starting index position, effectively returning
   * the navigator to its initial state when it was created.
   *
   * @throws RepositoryException if an error occurs during the reset operation
   */
  @Override
  public void reset() throws RepositoryException {
    logger.debug("Resetting commit navigator");
    currentIndex = startingIndex;
  }

  /**
   * Checks out the specified commit in the working directory.
   *
   * @param commitHash the hash of the commit to check out
   * @throws RepositoryException if the checkout operation fails
   */
  private void checkout(String commitHash) throws RepositoryException {
    logger.debug("Checking out commit: {}", commitHash);

    try {
      git.checkout().setName(commitHash).call();
    } catch (GitAPIException e) {
      throw new RepositoryException("Failed to checkout commit: " + commitHash, e);
    }
  }

  /**
   * Creates a CommitMetadata object from a RevCommit.
   *
   * @param commit the RevCommit to extract metadata from
   * @return a CommitMetadata object containing the commit information
   */
  private CommitMetadata createCommitMetadata(RevCommit commit) {
    var commitHash = commit.getName();
    var committerUsername = commit.getCommitterIdent().getName();
    var committerEmail = commit.getCommitterIdent().getEmailAddress();
    var commitDate = commit.getCommitterIdent().getWhen().toInstant();
    var commitMessage = commit.getFullMessage();
    var changedFiles = Lists.<String>newArrayList();

    return CommitMetadata.create(
        commitHash, committerUsername, committerEmail, commitDate, commitMessage, changedFiles);
  }
}
