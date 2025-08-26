package edu.stanford.protege.commitnavigator;

import edu.stanford.protege.commitnavigator.config.RepositoryConfig;
import edu.stanford.protege.commitnavigator.exceptions.GitHubNavigatorException;
import edu.stanford.protege.commitnavigator.utils.CommitNavigator;

/**
 * Primary interface for navigating GitHub repository commits with file filtering and authentication support.
 *
 * <p>This interface provides methods to initialize repository access, navigate through commits,
 * and manage repository synchronization. The navigator supports various authentication methods
 * and can filter commits based on file changes.</p>
 *
 * @since 1.0.0
 */
public interface GitHubRepository {

    /**
     * Initializes the repository navigator by cloning or opening the repository.
     *
     * @throws GitHubNavigatorException if initialization fails due to authentication,
     *                                  network issues, or repository access problems
     */
    void initialize() throws GitHubNavigatorException;

    /**
     * Retrieves the commit navigator for traversing repository commits.
     *
     * <p>The commit navigator provides methods to move through commits sequentially,
     * with optional file filtering applied based on the configuration.</p>
     *
     * @return a {@link CommitNavigator} instance for commit traversal
     * @throws GitHubNavigatorException if the repository is not initialized or
     *                                  if there are issues accessing commit history
     */
    CommitNavigator getCommitNavigator() throws GitHubNavigatorException;

    /**
     * Fetches the latest changes from the remote repository.
     *
     * <p>This method synchronizes the local repository with the remote by:</p>
     * <ul>
     *   <li>Fetching new commits from the remote branch</li>
     *   <li>Updating local references to match remote state</li>
     *   <li>Logging synchronization status and progress</li>
     * </ul>
     *
     * @throws GitHubNavigatorException if fetching fails due to authentication,
     *                                  network issues, or repository conflicts
     */
    void fetchLatestChanges() throws GitHubNavigatorException;

    /**
     * Closes the repository navigator and releases associated resources.
     *
     * <p>This method should be called when the navigator is no longer needed to
     * ensure proper cleanup of file handles, network connections, and temporary files.</p>
     *
     * @throws GitHubNavigatorException if cleanup operations fail
     */
    void close() throws GitHubNavigatorException;

    /**
     * Retrieves the configuration used to create this navigator.
     *
     * @return the {@link RepositoryConfig} instance containing repository URL,
     *         authentication settings, file filters, and other configuration options
     */
    RepositoryConfig getConfig();
}