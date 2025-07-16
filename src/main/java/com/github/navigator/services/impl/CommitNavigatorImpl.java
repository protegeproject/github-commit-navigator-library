package com.github.navigator.services.impl;

import com.github.navigator.config.NavigatorConfig;
import com.github.navigator.exceptions.RepositoryException;
import com.github.navigator.model.CommitMetadata;
import com.github.navigator.services.CommitNavigator;
import com.github.navigator.services.FileChangeDetector;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CommitNavigatorImpl implements CommitNavigator {
  private static final Logger logger = LoggerFactory.getLogger(CommitNavigatorImpl.class);

  private final Repository repository;
  private final Git git;
  private final NavigatorConfig config;
  private final FileChangeDetector fileChangeDetector;

  private List<RevCommit> filteredCommits;
  private int currentIndex;
  private boolean initialized = false;

  @Inject
  public CommitNavigatorImpl(Repository repository, Git git, NavigatorConfig config, FileChangeDetector fileChangeDetector) {
    this.repository = Objects.requireNonNull(repository, "Repository cannot be null");
    this.git = Objects.requireNonNull(git, "Git cannot be null");
    this.config = Objects.requireNonNull(config, "NavigatorConfig cannot be null");
    this.fileChangeDetector = Objects.requireNonNull(fileChangeDetector, "FileChangeDetector cannot be null");
    this.currentIndex = -1;
  }

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

  @Override
  public CommitMetadata nextAndCheckout() throws RepositoryException {
    var commitMetadata = next();
    if (commitMetadata != null) {
      checkout(commitMetadata.getCommitHash());
    }
    return commitMetadata;
  }

  @Override
  public CommitMetadata previousAndCheckout() throws RepositoryException {
    var commitMetadata = previous();
    if (commitMetadata != null) {
      checkout(commitMetadata.getCommitHash());
    }
    return commitMetadata;
  }

  @Override
  public boolean hasNext() throws RepositoryException {
    ensureInitialized();
    return currentIndex < filteredCommits.size() - 1;
  }

  @Override
  public boolean hasPrevious() throws RepositoryException {
    ensureInitialized();
    return currentIndex > 0;
  }

  @Override
  public CommitMetadata getCurrentCommit() throws RepositoryException {
    ensureInitialized();

    if (currentIndex >= 0 && currentIndex < filteredCommits.size()) {
      return createCommitMetadata(filteredCommits.get(currentIndex));
    }
    return null;
  }

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

      if (config.getStartingCommit().isPresent()) {
        var startingCommit = config.getStartingCommit().get();
        currentIndex = findCommitIndex(startingCommit);
        if (currentIndex == -1) {
          throw new RepositoryException("Starting commit not found: " + startingCommit);
        }
      } else {
        currentIndex = filteredCommits.size() - 1;
      }

      initialized = true;
      logger.debug("Navigator initialized with {} filtered commits, starting at index {}",
        filteredCommits.size(), currentIndex);

    } catch (Exception e) {
      throw new RepositoryException("Failed to initialize commit navigator", e);
    }
  }

  private List<RevCommit> buildFilteredCommitList() throws RepositoryException {
    logger.debug("Building filtered commit list");

    try (RevWalk revWalk = new RevWalk(repository)) {
      revWalk.markStart(revWalk.parseCommit(repository.resolve("HEAD")));

      var commits = new ArrayList<RevCommit>();
      var iterator = revWalk.iterator();

      while (iterator.hasNext()) {
        var commit = iterator.next();

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
    var fileFilters = config.getFileFilters();

    if (fileFilters == null || fileFilters.isEmpty()) {
      return true;
    }

    return fileChangeDetector.hasFileChanges(repository, commit, fileFilters);
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
    var commitDate = LocalDateTime.ofInstant(
      commit.getCommitterIdent().getWhen().toInstant(),
      ZoneId.systemDefault()
    );
    var commitMessage = commit.getFullMessage();

    return CommitMetadata.create(commitHash, committerUsername, commitDate, commitMessage);
  }
}