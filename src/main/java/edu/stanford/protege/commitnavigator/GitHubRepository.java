package edu.stanford.protege.commitnavigator;

import edu.stanford.protege.commitnavigator.config.RepositoryConfig;
import edu.stanford.protege.commitnavigator.exceptions.GitHubNavigatorException;
import edu.stanford.protege.commitnavigator.exceptions.RepositoryException;
import java.nio.file.Path;

/**
 * Primary interface for managing GitHub repository access with authentication support.
 *
 * <p>This interface provides methods to initialize repository access, manage repository
 * synchronization, and access the working directory. The repository supports various authentication
 * methods including personal access tokens and SSH keys for both public and private repositories.
 *
 * @since 1.0.0
 */
public interface GitHubRepository {

  /**
   * Initializes the repository by cloning or opening the repository.
   *
   * @throws GitHubNavigatorException if initialization fails due to authentication, network issues,
   *     or repository access problems
   */
  void initialize() throws GitHubNavigatorException;

  /**
   * Returns the working directory path of the initialized repository.
   *
   * @return the {@link Path} to the repository's working directory
   * @throws RepositoryException if the repository is not initialized or if there are issues
   *     accessing the working directory
   */
  Path getWorkingDirectory() throws RepositoryException;

  /**
   * Fetches the latest changes from the remote repository.
   *
   * <p>This method synchronizes the local repository with the remote by:
   *
   * <ul>
   *   <li>Fetching new commits from the remote branch
   *   <li>Updating local references to match remote state
   *   <li>Logging synchronization status and progress
   * </ul>
   *
   * @throws RepositoryException if fetching fails due to authentication, network issues, or
   *     repository conflicts
   */
  void fetchLatestChanges() throws RepositoryException;

  /**
   * Closes the repository and releases associated resources.
   *
   * <p>This method should be called when the repository is no longer needed to ensure proper
   * cleanup of file handles, network connections, and temporary files.
   *
   * @throws RepositoryException if cleanup operations fail
   */
  void close() throws RepositoryException;

  /**
   * Retrieves the configuration used to create this repository.
   *
   * @return the {@link RepositoryConfig} instance containing repository URL, authentication
   *     settings, and other repository-level configuration options
   */
  RepositoryConfig getConfig();
}
