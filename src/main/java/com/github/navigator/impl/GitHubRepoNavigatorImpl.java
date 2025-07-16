package com.github.navigator.impl;

import com.github.navigator.GitHubRepoNavigator;
import com.github.navigator.config.NavigatorConfig;
import com.github.navigator.exceptions.AuthenticationException;
import com.github.navigator.exceptions.GitHubNavigatorException;
import com.github.navigator.exceptions.RepositoryException;
import com.github.navigator.services.AuthenticationManager;
import com.github.navigator.services.CommitNavigator;
import com.github.navigator.services.FileChangeDetector;
import com.github.navigator.services.impl.CommitNavigatorImpl;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
    this.config = config;
    this.authenticationManager = authenticationManager;
    this.fileChangeDetector = fileChangeDetector;
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
      FetchCommand fetchCommand = git.fetch();
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
      Path localPath = getLocalRepositoryPath();

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

  private void createCommitNavigator() throws RepositoryException {
    logger.debug("Creating commit navigator");
    commitNavigator = new CommitNavigatorImpl(repository, git, config, fileChangeDetector);
  }

  private Path getLocalRepositoryPath() throws GitHubNavigatorException {
    if (config.getLocalCloneDirectory() != null) {
      return config.getLocalCloneDirectory();
    }

    try {
      String repoName = extractRepositoryName(config.getRepositoryUrl());
      Path tempDir = Files.createTempDirectory("github-navigator-");
      return tempDir.resolve(repoName);
    } catch (IOException e) {
      throw new GitHubNavigatorException("Failed to create temporary directory", e);
    }
  }

  private String extractRepositoryName(String repositoryUrl) {
    String[] parts = repositoryUrl.split("/");
    String repoName = parts[parts.length - 1];
    if (repoName.endsWith(".git")) {
      repoName = repoName.substring(0, repoName.length() - 4);
    }
    return repoName;
  }

  private boolean repositoryExists(Path localPath) {
    File gitDir = localPath.resolve(".git").toFile();
    return gitDir.exists() && gitDir.isDirectory();
  }

  private void openExistingRepository(Path localPath) throws IOException, GitAPIException {
    FileRepositoryBuilder builder = new FileRepositoryBuilder();
    repository = builder.setGitDir(localPath.resolve(".git").toFile())
      .readEnvironment()
      .findGitDir()
      .build();

    git = new Git(repository);

    logger.debug("Opened existing repository");
  }

  private void cloneRepository(Path localPath) throws GitAPIException, AuthenticationException, IOException {
    Files.createDirectories(localPath.getParent());

    CloneCommand cloneCommand = Git.cloneRepository();
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