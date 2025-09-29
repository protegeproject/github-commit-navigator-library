package edu.stanford.protege.commitnavigator.impl;

import edu.stanford.protege.commitnavigator.GitHubRepository;
import edu.stanford.protege.commitnavigator.config.RepositoryConfig;
import edu.stanford.protege.commitnavigator.exceptions.AuthenticationException;
import edu.stanford.protege.commitnavigator.exceptions.GitHubNavigatorException;
import edu.stanford.protege.commitnavigator.exceptions.RepositoryException;
import edu.stanford.protege.commitnavigator.utils.AuthenticationManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import javax.inject.Inject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link GitHubRepository} that provides Git repository management
 * functionality.
 *
 * <p>This implementation handles the complete lifecycle of repository operations including:
 *
 * <ul>
 *   <li>Repository cloning and opening existing repositories
 *   <li>GitHub authentication using various methods (personal access tokens, SSH keys)
 *   <li>Automatic synchronization with remote repositories
 *   <li>Working directory access for file operations
 *   <li>Resource management and cleanup
 * </ul>
 *
 * <p>The implementation uses JGit for Git operations and supports both authenticated and anonymous
 * access to repositories. It automatically handles repository setup by either cloning new
 * repositories or opening existing local repositories, and keeps them synchronized with the remote.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * var coordinates = RepositoryCoordinates.create("owner", "repository");
 * var repository = GitHubRepositoryBuilderFactory.create(coordinates)
 *     .withPersonalAccessToken("ghp_xxxxxxxxxxxx")
 *     .localWorkingDirectory("/tmp/my-repo")
 *     .build();
 *
 * repository.initialize();
 *
 * // Access working directory
 * Path workingDir = repository.getWorkingDirectory();
 *
 * // Fetch latest changes
 * repository.fetchLatestChanges();
 *
 * // Clean up resources
 * repository.close();
 * }</pre>
 *
 * <p>Thread Safety: This class is not thread-safe and should be used by a single thread or with
 * external synchronization.
 *
 * @since 1.0.0
 */
public class GitHubRepositoryImpl implements GitHubRepository {
  private static final Logger logger = LoggerFactory.getLogger(GitHubRepositoryImpl.class);

  private final RepositoryConfig config;
  private final AuthenticationManager authManager;

  private Repository repository;
  private Git git;
  private boolean initialized = false;

  /**
   * Constructs a new GitHubRepositoryImpl with the specified dependencies.
   *
   * <p>This constructor uses dependency injection to provide the required services for repository
   * operations and authentication.
   *
   * @param config the repository configuration containing repository URL, authentication, and other
   *     parameters
   * @param authManager the service for handling GitHub authentication
   * @throws NullPointerException if any parameter is null
   */
  @Inject
  public GitHubRepositoryImpl(RepositoryConfig config, AuthenticationManager authManager) {
    this.config = Objects.requireNonNull(config, "Repository config cannot be null");
    this.authManager = Objects.requireNonNull(authManager, "Authentication manager cannot be null");
  }

  /**
   * Initializes the repository by cloning or opening the repository.
   *
   * <p>This method performs the following operations:
   *
   * <ul>
   *   <li>Authenticates with GitHub using the provided credentials
   *   <li>Sets up the local Git repository: a) clones the repository if it doesn't exist locally or
   *       b) opens existing local repository if already cloned
   *   <li>Fetches latest changes from the remote repository
   * </ul>
   *
   * @throws GitHubNavigatorException if initialization fails due to authentication, network issues,
   *     or repository access problems
   */
  @Override
  public void initialize() throws GitHubNavigatorException {
    logger.info("Initializing GitHub repository for: {}", config.getCloneUrl());

    try {
      authenticateWithGitHub();
      setupRepository();

      initialized = true;
      logger.info("Successfully initialized GitHub repository");

    } catch (RepositoryException e) {
      cleanup();
      throw e;
    }
  }

  @Override
  public Path getWorkingDirectory() throws RepositoryException {
    ensureInitialized();
    return repository.getWorkTree().toPath();
  }

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
  @Override
  public void fetchLatestChanges() throws RepositoryException {
    ensureInitialized();

    logger.debug("Fetching latest changes from remote repository");

    try {
      var fetchCommand = git.fetch();
      if (config.getAuthConfig().isPresent()) {
        fetchCommand.setCredentialsProvider(
            authManager.getCredentialsProvider(config.getAuthConfig().get()));
      }
      fetchCommand.call();

      logger.debug("Successfully fetched latest changes");

    } catch (GitAPIException | AuthenticationException e) {
      throw new RepositoryException("Failed to fetch latest changes", e);
    }
  }

  /**
   * Closes the repository and releases all resources.
   *
   * <p>This method safely closes the Git repository, Git instance, and any other resources used by
   * the repository. It also resets the initialization state.
   *
   * @throws RepositoryException if an error occurs during cleanup
   */
  @Override
  public void close() throws RepositoryException {
    logger.info("Closing GitHub repository navigator");
    cleanup();
  }

  /**
   * Returns the configuration used by this repository.
   *
   * <p>This method provides access to the repository's configuration parameters including
   * repository URL, authentication settings, and other repository options.
   *
   * @return the {@link RepositoryConfig} used by this repository
   */
  @Override
  public RepositoryConfig getConfig() {
    return config;
  }

  /**
   * Authenticates with GitHub using the configured authentication method.
   *
   * <p>If authentication configuration is provided, it will be validated and used for
   * authentication. Otherwise, anonymous access will be attempted for public repositories.
   *
   * @throws AuthenticationException if authentication fails
   */
  private void authenticateWithGitHub() throws AuthenticationException {
    if (config.getAuthConfig().isPresent()) {
      logger.debug("Authenticating with GitHub using provided credentials");

      authManager.validateAuthentication(config.getAuthConfig().get());
      authManager.authenticateGitHub(config.getAuthConfig().get());

      logger.debug("Successfully authenticated with GitHub");
    } else {
      logger.debug(
          "No authentication provided, connecting to GitHub anonymously for public repository access");
      authManager.authenticateGitHub(null);
    }
  }

  /**
   * Sets up the local Git repository by either opening an existing repository or cloning a new one
   * from the remote URL.
   *
   * @throws RepositoryException if repository setup fails
   */
  private void setupRepository() throws RepositoryException {
    try {
      var localPath = getLocalRepositoryPath();

      if (repositoryExists(localPath)) {
        logger.info("Opening existing repository at: {}", localPath);
        openExistingRepository(localPath);
      } else {
        logger.info("Cloning repository to: {}", localPath);
        cloneRepository(localPath);
      }

    } catch (IOException | GitAPIException e) {
      throw new RepositoryException("Failed to setup repository", e);
    } catch (AuthenticationException e) {
      throw new RepositoryException("Authentication failed during repository setup", e);
    }
  }

  /**
   * Determines the local path where the repository should be stored.
   *
   * <p>Uses the configured local clone directory if provided, otherwise creates a temporary
   * directory for the repository.
   *
   * @return the local repository path
   * @throws RepositoryException if the path cannot be determined or created
   */
  private Path getLocalRepositoryPath() throws RepositoryException {
    if (config.getLocalWorkingDirectory() != null) {
      return config.getLocalWorkingDirectory();
    }

    try {
      var repoName = config.getRepositoryName();
      var tempDir = Files.createTempDirectory("github-navigator-");
      return tempDir.resolve(repoName);
    } catch (IOException e) {
      throw new RepositoryException("Failed to create temporary directory", e);
    }
  }

  /**
   * Checks if a Git repository exists at the specified local path.
   *
   * @param localPath the local path to check
   * @return true if a Git repository exists at the path, false otherwise
   */
  private boolean repositoryExists(Path localPath) {
    var gitDir = localPath.resolve(".git").toFile();
    return gitDir.exists() && gitDir.isDirectory();
  }

  /**
   * Opens an existing Git repository at the specified path and synchronizes with remote.
   *
   * @param localPath the path to the existing repository
   * @throws IOException if I/O operations fail
   * @throws GitAPIException if Git operations fail
   * @throws AuthenticationException if authentication fails during sync
   */
  private void openExistingRepository(Path localPath)
      throws IOException, GitAPIException, AuthenticationException {
    var builder = new FileRepositoryBuilder();
    repository = builder.setGitDir(localPath.resolve(".git").toFile()).readEnvironment().build();
    git = new Git(repository);
    logger.debug("Opened existing repository");

    // Check for and pull new changes from remote
    pullLatestChanges();
  }

  /**
   * Pulls the latest changes from the remote repository if available.
   *
   * <p>This method first fetches the latest remote references, then compares the local and remote
   * commit hashes to determine if a pull is needed.
   *
   * @throws GitAPIException if Git operations fail
   * @throws AuthenticationException if authentication fails
   */
  private void pullLatestChanges() throws GitAPIException, AuthenticationException {
    try {
      logger.info("Checking for remote changes...");

      // First fetch to get latest remote refs
      var fetchCommand = git.fetch();
      if (config.getAuthConfig().isPresent()) {
        fetchCommand.setCredentialsProvider(
            authManager.getCredentialsProvider(config.getAuthConfig().get()));
      }
      fetchCommand.call();

      // Check if there are any new commits
      var currentBranch = repository.getBranch();
      var localRef = repository.findRef("HEAD");
      var remoteRef = repository.findRef("refs/remotes/origin/" + currentBranch);

      if (localRef != null
          && remoteRef != null
          && !localRef.getObjectId().equals(remoteRef.getObjectId())) {

        logger.info(
            "New changes detected on remote branch '{}', pulling changes...", currentBranch);

        // Pull the changes
        var pullCommand = git.pull();
        if (config.getAuthConfig().isPresent()) {
          pullCommand.setCredentialsProvider(
              authManager.getCredentialsProvider(config.getAuthConfig().get()));
        }
        var pullResult = pullCommand.call();

        if (pullResult.isSuccessful()) {
          logger.info(
              "Successfully pulled {} new commits from remote",
              pullResult.getFetchResult().getTrackingRefUpdates().size());
        } else {
          logger.warn(
              "Pull completed but may have conflicts. Merge result: {}",
              pullResult.getMergeResult().getMergeStatus());
        }
      } else {
        logger.info("Repository is up to date with remote");
      }

    } catch (IOException e) {
      logger.warn("Failed to check for remote changes: {}", e.getMessage());
      // Don't throw exception here as opening the repository should still succeed
    }
  }

  /**
   * Clones the remote repository to the specified local path.
   *
   * <p>This method creates the necessary parent directories and configures the clone operation
   * based on the navigator configuration including branch, authentication, and shallow clone
   * settings.
   *
   * @param localPath the local path where the repository should be cloned
   * @throws GitAPIException if Git operations fail
   * @throws AuthenticationException if authentication fails
   * @throws IOException if I/O operations fail
   */
  private void cloneRepository(Path localPath)
      throws GitAPIException, AuthenticationException, IOException {
    var cloneCommand = Git.cloneRepository();
    cloneCommand.setURI(config.getCloneUrl());
    cloneCommand.setDirectory(localPath.toFile());
    cloneCommand.setBranch(config.getBranch());

    if (config.getAuthConfig().isPresent()) {
      cloneCommand.setCredentialsProvider(
          authManager.getCredentialsProvider(config.getAuthConfig().get()));
    }

    if (config.isShallowClone()) {
      cloneCommand.setDepth(1);
    }

    git = cloneCommand.call();
    repository = git.getRepository();

    logger.debug("Successfully cloned repository");
  }

  /**
   * Ensures that the repository has been properly initialized before use.
   *
   * @throws RepositoryException if the repository is not initialized
   */
  private void ensureInitialized() throws RepositoryException {
    if (!initialized) {
      throw new RepositoryException("Repository not initialized. Call initialize() first.");
    }
  }

  /**
   * Cleans up resources including Git repository and related objects.
   *
   * <p>This method safely closes Git and repository resources and resets the initialization state.
   * It logs warnings for any cleanup errors but does not throw exceptions.
   */
  private void cleanup() {
    try {
      if (git != null) {
        git.close();
      }
      if (repository != null) {
        repository.close();
      }
    } catch (Exception e) {
      logger.warn("Error during cleanup", e);
    }

    initialized = false;
  }
}
