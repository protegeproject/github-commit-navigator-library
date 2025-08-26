package edu.stanford.protege.commitnavigator.impl;

import edu.stanford.protege.commitnavigator.GitHubRepository;
import edu.stanford.protege.commitnavigator.config.RepositoryConfig;
import edu.stanford.protege.commitnavigator.exceptions.AuthenticationException;
import edu.stanford.protege.commitnavigator.exceptions.GitHubNavigatorException;
import edu.stanford.protege.commitnavigator.exceptions.RepositoryException;
import edu.stanford.protege.commitnavigator.utils.AuthenticationManager;
import edu.stanford.protege.commitnavigator.utils.CommitNavigator;
import edu.stanford.protege.commitnavigator.utils.FileChangeDetector;
import edu.stanford.protege.commitnavigator.utils.impl.CommitNavigatorImpl;
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
 * Default implementation of {@link GitHubRepository} that provides Git repository management and
 * commit navigation functionality.
 *
 * <p>This implementation handles the complete lifecycle of repository operations including:
 *
 * <ul>
 *   <li>Repository cloning and opening existing repositories
 *   <li>GitHub authentication using various methods
 *   <li>Automatic synchronization with remote repositories
 *   <li>Commit navigation with file filtering support
 *   <li>Resource management and cleanup
 * </ul>
 *
 * <p>The implementation uses JGit for Git operations and the GitHub API for authentication and
 * remote repository access. It supports both shallow and full clones, and automatically pulls the
 * latest changes when opening existing repositories.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * var coordinate = RepositoryCoordinate.create(repoName, repoName);
 * var repository = GitHubRepositoryBuilderFactory.create(coordinate)
 *     .withPersonalAccessToken("token")
 *     .fileFilters("*.java")
 *     .build();
 *
 * repository.initialize();
 * var commitNavigator = repository.getCommitNavigator();
 *
 * while (commitNavigator.hasNext()) {
 *     var commit = commitNavigator.next();
 *     // Process commit
 * }
 *
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
  private final FileChangeDetector fileChangeDetector;

  private Repository repository;
  private Git git;
  private CommitNavigator commitNavigator;
  private boolean initialized = false;

  /**
   * Constructs a new GitHubRepoNavigatorImpl with the specified dependencies.
   *
   * <p>This constructor uses dependency injection to provide the required services for repository
   * operations, authentication, and file change detection.
   *
   * @param config the repository configuration containing repository URL, authentication, and other
   *     parameters
   * @param authManager the service for handling GitHub authentication
   * @param fileChangeDetector the service for detecting file changes in commits
   * @throws NullPointerException if any parameter is null
   */
  @Inject
  public GitHubRepositoryImpl(
      RepositoryConfig config,
      AuthenticationManager authManager,
      FileChangeDetector fileChangeDetector) {
    this.config = Objects.requireNonNull(config, "Repository config cannot be null");
    this.authManager = Objects.requireNonNull(authManager, "Authentication manager cannot be null");
    this.fileChangeDetector =
        Objects.requireNonNull(fileChangeDetector, "File change detector cannot be null");
  }

  /**
   * Initializes the repository navigator by cloning or opening the repository.
   *
   * <p>This method performs the following operations:
   *
   * <ul>
   *   <li>Authenticates with GitHub using the provided credentials
   *   <li>Sets up the local Git repository: a) clones the repository if it doesn't exist locally or
   *       b) opens existing local repository if already cloned
   *   <li>Fetches latest changes from the remote repository
   *   <li>Creates a commit navigator instance for traversing repository commits
   * </ul>
   *
   * @throws GitHubNavigatorException if initialization fails due to authentication, network issues,
   *     or repository access problems
   */
  @Override
  public void initialize() throws GitHubNavigatorException {
    logger.info("Initializing GitHub repository navigator for: {}", config.getRepositoryUrl());

    try {
      authenticateWithGitHub();
      setupRepository();
      createCommitNavigator();

      initialized = true;
      logger.info("Successfully initialized GitHub repository navigator");

    } catch (GitHubNavigatorException e) {
      cleanup();
      throw e;
    }
  }

  /**
   * Retrieves the commit navigator for traversing repository commits.
   *
   * @return a {@link CommitNavigator} instance for commit traversal
   */
  @Override
  public CommitNavigator getCommitNavigator() throws GitHubNavigatorException {
    ensureInitialized();
    return commitNavigator;
  }

  /**
   * Fetches the latest changes from the remote repository.
   *
   * <p>This method synchronizes the local repository with the remote by fetching new commits and
   * updating local references. It also resets the commit navigator to reflect the latest state of
   * the repository.
   *
   * @throws GitHubNavigatorException if fetching fails due to authentication, network issues, or
   *     repository conflicts
   */
  @Override
  public void fetchLatestChanges() throws GitHubNavigatorException {
    ensureInitialized();

    logger.debug("Fetching latest changes from remote repository");

    try {
      var fetchCommand = git.fetch();
      if (config.getAuthConfig().isPresent()) {
        fetchCommand.setCredentialsProvider(
            authManager.getCredentialsProvider(config.getAuthConfig().get()));
      }
      fetchCommand.call();

      commitNavigator.reset();

      logger.debug("Successfully fetched latest changes");

    } catch (GitAPIException | AuthenticationException e) {
      throw new GitHubNavigatorException("Failed to fetch latest changes", e);
    } catch (RepositoryException e) {
      throw new GitHubNavigatorException("Failed to reset navigator after fetch", e);
    }
  }

  /**
   * Closes the navigator and releases all resources.
   *
   * <p>This method safely closes the Git repository, Git instance, and any other resources used by
   * the navigator. It also resets the initialization state.
   *
   * @throws GitHubNavigatorException if an error occurs during cleanup
   */
  @Override
  public void close() throws GitHubNavigatorException {
    logger.debug("Closing GitHub repository navigator");
    cleanup();
  }

  /**
   * Returns the configuration used by this navigator.
   *
   * <p>This method provides access to the navigator's configuration parameters including repository
   * URL, authentication settings, and other navigation options.
   *
   * @return the {@link RepositoryConfig} used by this navigator
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
   * @throws GitHubNavigatorException if repository setup fails
   */
  private void setupRepository() throws GitHubNavigatorException {
    try {
      var localPath = getLocalRepositoryPath();

      if (repositoryExists(localPath)) {
        logger.debug("Opening existing repository at: {}", localPath);
        openExistingRepository(localPath);
      } else {
        logger.debug("Cloning repository to: {}", localPath);
        cloneRepository(localPath);
      }

    } catch (IOException | GitAPIException e) {
      throw new GitHubNavigatorException("Failed to setup repository", e);
    } catch (AuthenticationException e) {
      throw new GitHubNavigatorException("Authentication failed during repository setup", e);
    }
  }

  /** Creates a commit navigator instance for traversing repository commits. */
  private void createCommitNavigator() {
    logger.debug("Creating commit navigator");
    commitNavigator = new CommitNavigatorImpl(repository, git, config, fileChangeDetector);
  }

  /**
   * Determines the local path where the repository should be stored.
   *
   * <p>Uses the configured local clone directory if provided, otherwise creates a temporary
   * directory for the repository.
   *
   * @return the local repository path
   * @throws GitHubNavigatorException if the path cannot be determined or created
   */
  private Path getLocalRepositoryPath() throws GitHubNavigatorException {
    if (config.getLocalCloneDirectory() != null) {
      return config.getLocalCloneDirectory();
    }

    try {
      var repoName = extractRepositoryName(config.getRepositoryUrl());
      var tempDir = Files.createTempDirectory("github-navigator-");
      return tempDir.resolve(repoName);
    } catch (IOException e) {
      throw new GitHubNavigatorException("Failed to create temporary directory", e);
    }
  }

  /**
   * Extracts the repository name from a GitHub repository URL.
   *
   * @param repositoryUrl the GitHub repository URL
   * @return the repository name without the .git suffix
   */
  private String extractRepositoryName(String repositoryUrl) {
    var parts = repositoryUrl.split("/");
    var repoName = parts[parts.length - 1];
    if (repoName.endsWith(".git")) {
      repoName = repoName.substring(0, repoName.length() - 4);
    }
    return repoName;
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
    repository =
        builder
            .setGitDir(localPath.resolve(".git").toFile())
            .readEnvironment()
            .findGitDir()
            .build();

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
      logger.debug("Checking for remote changes...");

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
        logger.debug("Repository is up to date with remote");
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
    cloneCommand.setURI(config.getRepositoryUrl());
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
   * Ensures that the navigator has been properly initialized before use.
   *
   * @throws GitHubNavigatorException if the navigator is not initialized
   */
  private void ensureInitialized() throws GitHubNavigatorException {
    if (!initialized) {
      throw new GitHubNavigatorException("Navigator not initialized. Call initialize() first.");
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
