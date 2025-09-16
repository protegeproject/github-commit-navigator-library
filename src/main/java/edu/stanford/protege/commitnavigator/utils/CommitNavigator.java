package edu.stanford.protege.commitnavigator.utils;

import edu.stanford.protege.commitnavigator.exceptions.RepositoryException;
import edu.stanford.protege.commitnavigator.model.CommitMetadata;
import java.nio.file.Path;

/**
 * Service interface for navigating through Git repository commits in sequential order.
 *
 * <p>This interface provides methods to move forward and backward through commit history, with
 * optional checkout functionality. The navigation respects file filters and branch configuration
 * specified in the repository navigator configuration.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * var navigator = commitNavigator;
 *
 * // Navigate to child commits (newer)
 * while (navigator.hasChild()) {
 *     var commit = navigator.fetchChild();
 *     System.out.println("Processing commit: " + commit.getCommitHash());
 * }
 *
 * // Navigate to parent commits (older) with checkout
 * navigator.reset();
 * while (navigator.hasParent()) {
 *     var commit = navigator.pullParent();
 *     System.out.println("Checked out commit: " + commit.getCommitHash());
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface CommitNavigator {

  /**
   * Checks out the child commit to the working directory.
   *
   * @return the {@link CommitMetadata} of the child commit
   * @throws RepositoryException if there is no child commit, if checkout fails, or if an error
   *     occurs during navigation
   */
  CommitMetadata checkoutChild() throws RepositoryException;

  /**
   * Checks out the parent commit to the working directory.
   *
   * @return the {@link CommitMetadata} of the parent commit
   * @throws RepositoryException if there is no parent commit, if checkout fails, or if an error
   *     occurs during navigation
   */
  CommitMetadata checkoutParent() throws RepositoryException;

  /**
   * Checks if there is a child commit available for navigation.
   *
   * @return true if there is a child commit, false otherwise
   * @throws RepositoryException if an error occurs while checking for the child commit
   */
  boolean hasChild() throws RepositoryException;

  /**
   * Checks if there is a parent commit available for navigation.
   *
   * @return true if there is a parent commit, false otherwise
   * @throws RepositoryException if an error occurs while checking for the parent commit
   */
  boolean hasParent() throws RepositoryException;

  /**
   * Returns the total number of commits available in the navigation sequence.
   *
   * @return the total commit count
   * @throws RepositoryException if an error occurs while getting for the total commit count
   */
  long getCommitCount() throws RepositoryException;

  /**
   * Returns the metadata of the current commit without changing the navigation position.
   *
   * @return the {@link CommitMetadata} of the current commit
   * @throws RepositoryException if there is no current commit or if an error occurs while
   *     retrieving commit metadata
   */
  CommitMetadata getCurrentCommit() throws RepositoryException;

  /**
   * Returns the absolute path on the local filesystem given a file path relative to the repository
   * root.
   *
   * @param relativePath the relative file path within the repository (e.g.,
   *     "src/main/java/Main.java")
   * @return the absolute {@link Path} to the file on the local filesystem
   */
  Path resolveFilePath(String relativePath);

  /**
   * Resets the navigator to its initial state, typically the starting commit or the first commit.
   *
   * @throws RepositoryException if an error occurs while resetting the navigation state
   */
  void reset() throws RepositoryException;
}
