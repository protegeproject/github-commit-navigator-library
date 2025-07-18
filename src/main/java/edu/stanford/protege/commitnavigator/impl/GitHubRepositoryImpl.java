package edu.stanford.protege.commitnavigator.impl;

import edu.stanford.protege.commitnavigator.GitHubRepository;
import edu.stanford.protege.commitnavigator.config.RepositoryConfig;
import edu.stanford.protege.commitnavigator.exceptions.AuthenticationException;
import edu.stanford.protege.commitnavigator.exceptions.GitHubNavigatorException;
import edu.stanford.protege.commitnavigator.exceptions.RepositoryException;
import edu.stanford.protege.commitnavigator.services.AuthenticationManager;
import edu.stanford.protege.commitnavigator.services.CommitNavigator;
import edu.stanford.protege.commitnavigator.services.FileChangeDetector;
import edu.stanford.protege.commitnavigator.services.impl.CommitNavigatorImpl;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Default implementation of {@link GitHubRepository} that provides Git repository management
 * and commit navigation functionality.
 * 
 * <p>This implementation handles the complete lifecycle of repository operations including:</p>
 * <ul>
 *   <li>Repository cloning and opening existing repositories</li>
 *   <li>GitHub authentication using various methods</li>
 *   <li>Automatic synchronization with remote repositories</li>
 *   <li>Commit navigation with file filtering support</li>
 *   <li>Resource management and cleanup</li>
 * </ul>
 * 
 * <p>The implementation uses JGit for Git operations and the GitHub API for authentication
 * and remote repository access. It supports both shallow and full clones, and automatically
 * pulls the latest changes when opening existing repositories.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 * {@code
 * var navigator = GitHubRepoNavigatorBuilder
 *     .forRepository("https://github.com/user/repo.git")
 *     .withPersonalAccessToken("token")
 *     .fileFilters("*.java")
 *     .build();
 *
 * navigator.initialize();
 * var commitNavigator = navigator.getCommitNavigator();
 *
 * while (commitNavigator.hasNext()) {
 *     var commit = commitNavigator.next();
 *     // Process commit
 * }
 *
 * navigator.close();
 * }
 * </pre>
 * 
 * <p>Thread Safety: This class is not thread-safe and should be used by a single thread
 * or with external synchronization.</p>
 * 
 * @since 1.0.0
 */
public class GitHubRepositoryImpl implements GitHubRepository {
  private static final Logger logger = LoggerFactory.getLogger(GitHubRepositoryImpl.class);

  private final RepositoryConfig config;
  private final AuthenticationManager authenticationManager;
  private final FileChangeDetector fileChangeDetector;

  private Repository repository;
  private Git git;
  private GitHub github;
  private CommitNavigator commitNavigator;
  private boolean initialized = false;

  /**
   * Constructs a new GitHubRepoNavigatorImpl with the specified dependencies.
   * 
   * <p>This constructor uses dependency injection to provide the required services
   * for repository operations, authentication, and file change detection.</p>
   * 
   * @param config the navigation configuration containing repository URL, authentication,
   *               and other navigation parameters
   * @param authenticationManager the service for handling GitHub authentication
   * @param fileChangeDetector the service for detecting file changes in commits
   * @throws NullPointerException if any parameter is null
   */
  @Inject
  public GitHubRepositoryImpl(RepositoryConfig config,
                              AuthenticationManager authenticationManager,
                              FileChangeDetector fileChangeDetector) {
    this.config = Objects.requireNonNull(config, "NavigatorConfig cannot be null");
    this.authenticationManager = Objects.requireNonNull(authenticationManager, "AuthenticationManager cannot be null");
    this.fileChangeDetector = Objects.requireNonNull(fileChangeDetector, "FileChangeDetector cannot be null");
  }

  /**
   * Initializes the repository navigator by cloning or opening the repository.
   *
   * <p>This method performs the following operations:</p>
   * <ul>
   *   <li>Validates the configuration parameters</li>
   *   <li>Authenticates with GitHub using the provided credentials</li>
   *   <li>Sets up the local Git repository: a) clones the repository if it doesn't exist locally
   *       or b) opens existing local repository if already cloned</li>
   *   <li>Fetches latest changes from the remote repository</li>
   *   <li>Creates a commit navigator instance for traversing repository commits</li>
   * </ul>
   *
   * @throws GitHubNavigatorException if initialization fails due to authentication,
   *                                  network issues, or repository access problems
   */
  @Override
  public void initialize() throws GitHubNavigatorException {
    logger.info("Initializing GitHub repository navigator for: {}", config.getRepositoryUrl());

    try {
      validateConfig();
      authenticateWithGitHub();
      setupRepository();
      createCommitNavigator();

      initialized = true;
      logger.info("Successfully initialized GitHub repository navigator");

    } catch (Exception e) {
      cleanup();
      throw new GitHubNavigatorException("Failed to initialize repository navigator", e);
    }
  }

  /**
   * Retrieves the commit navigator for traversing repository commits.
   *
   * @return a {@link CommitNavigator} instance for commit traversal
   * @throws GitHubNavigatorException
   */
  @Override
  public CommitNavigator getCommitNavigator() throws GitHubNavigatorException {
    ensureInitialized();
    return commitNavigator;
  }

  /**
   * Fetches the latest changes from the remote repository.
   *
   * <p>This method synchronizes the local repository with the remote by fetching
   * new commits and updating local references. It also resets the commit navigator
   * to reflect the latest state of the repository.</p>
   *
   * @throws GitHubNavigatorException if fetching fails due to authentication,
   *                                  network issues, or repository conflicts
   */
  @Override
  public void fetchLatestChanges() throws GitHubNavigatorException {
    ensureInitialized();

    logger.debug("Fetching latest changes from remote repository");

    try {
      var fetchCommand = git.fetch();
      if (config.getAuthConfig().isPresent()) {
        fetchCommand.setCredentialsProvider(
          authenticationManager.getCredentialsProvider(config.getAuthConfig().get())
        );
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
   * <p>This method safely closes the Git repository, Git instance, and any other
   * resources used by the navigator. It also resets the initialization state.</p>
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
   * <p>This method provides access to the navigator's configuration parameters
   * including repository URL, authentication settings, and other navigation options.</p>
   *
   * @return the {@link RepositoryConfig} used by this navigator
   */
  @Override
  public RepositoryConfig getConfig() {
    return config;
  }

  /**
   * Validates the navigator configuration to ensure all required parameters are present.
   * 
   * @throws GitHubNavigatorException if the configuration is invalid
   */
  private void validateConfig() throws GitHubNavigatorException {
    if (config == null) {
      throw new GitHubNavigatorException("Configuration cannot be null");
    }

    if (config.getRepositoryUrl() == null || config.getRepositoryUrl().trim().isEmpty()) {
      throw new GitHubNavigatorException("Repository URL cannot be null or empty");
    }

    // Authentication is optional for public repositories
    logger.debug("Authentication config present: {}", config.getAuthConfig().isPresent());
  }

  /**
   * Authenticates with GitHub using the configured authentication method.
   * 
   * <p>If authentication configuration is provided, it will be validated and used
   * for authentication. Otherwise, anonymous access will be attempted for public repositories.</p>
   * 
   * @throws AuthenticationException if authentication fails
   */
  private void authenticateWithGitHub() throws AuthenticationException {
    if (config.getAuthConfig().isPresent()) {
      logger.debug("Authenticating with GitHub using provided credentials");

      authenticationManager.validateAuthentication(config.getAuthConfig().get());
      github = authenticationManager.authenticateGitHub(config.getAuthConfig().get());

      logger.debug("Successfully authenticated with GitHub");
    } else {
      logger.debug("No authentication provided, connecting to GitHub anonymously for public repository access");
      github = authenticationManager.authenticateGitHub(null);
    }
  }

  /**
   * Sets up the local Git repository by either opening an existing repository
   * or cloning a new one from the remote URL.
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

    } catch (Exception e) {
      throw new GitHubNavigatorException("Failed to setup repository", e);
    }
  }

  /**
   * Creates a commit navigator instance for traversing repository commits.
   */
  private void createCommitNavigator() {
    logger.debug("Creating commit navigator");
    commitNavigator = new CommitNavigatorImpl(repository, git, config, fileChangeDetector);
  }

  /**
   * Determines the local path where the repository should be stored.
   * 
   * <p>Uses the configured local clone directory if provided, otherwise creates
   * a temporary directory for the repository.</p>
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
  private void openExistingRepository(Path localPath) throws IOException, GitAPIException, AuthenticationException {
    var builder = new FileRepositoryBuilder();
    repository = builder.setGitDir(localPath.resolve(".git").toFile())
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
   * <p>This method first fetches the latest remote references, then compares
   * the local and remote commit hashes to determine if a pull is needed.</p>
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
          authenticationManager.getCredentialsProvider(config.getAuthConfig().get())
        );
      }
      fetchCommand.call();
      
      // Check if there are any new commits
      var currentBranch = repository.getBranch();
      var localRef = repository.findRef("HEAD");
      var remoteRef = repository.findRef("refs/remotes/origin/" + currentBranch);
      
      if (localRef != null && remoteRef != null && 
          !localRef.getObjectId().equals(remoteRef.getObjectId())) {
        
        logger.info("New changes detected on remote branch '{}', pulling changes...", currentBranch);
        
        // Pull the changes
        var pullCommand = git.pull();
        if (config.getAuthConfig().isPresent()) {
          pullCommand.setCredentialsProvider(
            authenticationManager.getCredentialsProvider(config.getAuthConfig().get())
          );
        }
        var pullResult = pullCommand.call();
        
        if (pullResult.isSuccessful()) {
          logger.info("Successfully pulled {} new commits from remote", 
                     pullResult.getFetchResult().getTrackingRefUpdates().size());
        } else {
          logger.warn("Pull completed but may have conflicts. Merge result: {}", 
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
   * <p>This method creates the necessary parent directories and configures
   * the clone operation based on the navigator configuration including branch,
   * authentication, and shallow clone settings.</p>
   * 
   * @param localPath the local path where the repository should be cloned
   * @throws GitAPIException if Git operations fail
   * @throws AuthenticationException if authentication fails
   * @throws IOException if I/O operations fail
   */
  private void cloneRepository(Path localPath) throws GitAPIException, AuthenticationException, IOException {
    Files.createDirectories(localPath.getParent());

    var cloneCommand = Git.cloneRepository();
    cloneCommand.setURI(config.getRepositoryUrl());
    cloneCommand.setDirectory(localPath.toFile());
    cloneCommand.setBranch(config.getBranch());

    if (config.getAuthConfig().isPresent()) {
      cloneCommand.setCredentialsProvider(
        authenticationManager.getCredentialsProvider(config.getAuthConfig().get())
      );
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
   * <p>This method safely closes Git and repository resources and resets
   * the initialization state. It logs warnings for any cleanup errors
   * but does not throw exceptions.</p>
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