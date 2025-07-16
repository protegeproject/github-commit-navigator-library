package edu.stanford.protege.webprotege.impl;

import edu.stanford.protege.webprotege.GitHubRepoNavigator;
import edu.stanford.protege.webprotege.config.NavigatorConfig;
import edu.stanford.protege.webprotege.exceptions.AuthenticationException;
import edu.stanford.protege.webprotege.exceptions.GitHubNavigatorException;
import edu.stanford.protege.webprotege.exceptions.RepositoryException;
import edu.stanford.protege.webprotege.services.AuthenticationManager;
import edu.stanford.protege.webprotege.services.CommitNavigator;
import edu.stanford.protege.webprotege.services.FileChangeDetector;
import edu.stanford.protege.webprotege.services.impl.CommitNavigatorImpl;
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

public class GitHubRepoNavigatorImpl implements GitHubRepoNavigator {
  private static final Logger logger = LoggerFactory.getLogger(GitHubRepoNavigatorImpl.class);

  private final NavigatorConfig config;
  private final AuthenticationManager authenticationManager;
  private final FileChangeDetector fileChangeDetector;

  private Repository repository;
  private Git git;
  private GitHub github;
  private CommitNavigator commitNavigator;
  private boolean initialized = false;

  @Inject
  public GitHubRepoNavigatorImpl(NavigatorConfig config,
                                 AuthenticationManager authenticationManager,
                                 FileChangeDetector fileChangeDetector) {
    this.config = Objects.requireNonNull(config, "NavigatorConfig cannot be null");
    this.authenticationManager = Objects.requireNonNull(authenticationManager, "AuthenticationManager cannot be null");
    this.fileChangeDetector = Objects.requireNonNull(fileChangeDetector, "FileChangeDetector cannot be null");
  }

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

  @Override
  public CommitNavigator getCommitNavigator() throws GitHubNavigatorException {
    ensureInitialized();
    return commitNavigator;
  }

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

  @Override
  public void close() throws GitHubNavigatorException {
    logger.debug("Closing GitHub repository navigator");
    cleanup();
  }

  @Override
  public NavigatorConfig getConfig() {
    return config;
  }

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

  private void createCommitNavigator() {
    logger.debug("Creating commit navigator");
    commitNavigator = new CommitNavigatorImpl(repository, git, config, fileChangeDetector);
  }

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

  private String extractRepositoryName(String repositoryUrl) {
    var parts = repositoryUrl.split("/");
    var repoName = parts[parts.length - 1];
    if (repoName.endsWith(".git")) {
      repoName = repoName.substring(0, repoName.length() - 4);
    }
    return repoName;
  }

  private boolean repositoryExists(Path localPath) {
    var gitDir = localPath.resolve(".git").toFile();
    return gitDir.exists() && gitDir.isDirectory();
  }

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

  private void ensureInitialized() throws GitHubNavigatorException {
    if (!initialized) {
      throw new GitHubNavigatorException("Navigator not initialized. Call initialize() first.");
    }
  }

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