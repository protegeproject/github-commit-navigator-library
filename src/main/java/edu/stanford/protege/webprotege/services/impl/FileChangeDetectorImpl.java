package edu.stanford.protege.webprotege.services.impl;

import edu.stanford.protege.webprotege.exceptions.RepositoryException;
import edu.stanford.protege.webprotege.services.FileChangeDetector;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Singleton
public class FileChangeDetectorImpl implements FileChangeDetector {
  private static final Logger logger = LoggerFactory.getLogger(FileChangeDetectorImpl.class);

  @Override
  public boolean hasFileChanges(Repository repository, RevCommit commit, List<String> fileFilters) throws RepositoryException {
    Objects.requireNonNull(repository, "Repository cannot be null");
    Objects.requireNonNull(commit, "Commit cannot be null");
    
    if (fileFilters == null || fileFilters.isEmpty()) {
      return true;
    }
    
    try {
      var changedFiles = getChangedFiles(repository, commit);
      return changedFiles.stream().anyMatch(file -> matchesAnyFilter(file, fileFilters));
    } catch (Exception e) {
      throw new RepositoryException("Failed to check file changes in commit " + commit.getName(), e);
    }
  }

  @Override
  public List<String> getChangedFiles(Repository repository, RevCommit commit) throws RepositoryException {
    Objects.requireNonNull(repository, "Repository cannot be null");
    Objects.requireNonNull(commit, "Commit cannot be null");
    
    logger.debug("Getting changed files for commit: {}", commit.getName());
    
    try (var revWalk = new RevWalk(repository);
         var diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
      
      diffFormatter.setRepository(repository);
      
      var parents = commit.getParents();
      var diffs = new ArrayList<DiffEntry>();
      
      if (parents.length == 0) {
        diffs.addAll(diffFormatter.scan(getEmptyTreeIterator(repository), 
                                       getTreeIterator(repository, commit)));
      } else {
        for (var parent : parents) {
          revWalk.parseCommit(parent);
          var parentDiffs = diffFormatter.scan(
            getTreeIterator(repository, parent),
            getTreeIterator(repository, commit)
          );
          diffs.addAll(parentDiffs);
        }
      }
      
      return diffs.stream()
              .map(this::getFilePath)
              .filter(path -> Optional.ofNullable(path)
                                    .map(String::trim)
                                    .filter(p -> !p.isEmpty())
                                    .isPresent())
              .toList();
      
    } catch (IOException e) {
      throw new RepositoryException("Failed to get changed files for commit " + commit.getName(), e);
    }
  }

  boolean matchesAnyFilter(String filePath, List<String> fileFilters) {
    if (fileFilters == null || fileFilters.isEmpty()) {
      return true;
    }
    return fileFilters.stream().anyMatch(filter -> matchesFilter(filePath, filter));
  }

  boolean matchesFilter(String filePath, String filter) {
    if (filter.contains("*") || filter.contains("?")) {
      try {
        var matcher = FileSystems.getDefault().getPathMatcher("glob:" + filter);
        return matcher.matches(FileSystems.getDefault().getPath(filePath));
      } catch (Exception e) {
        logger.warn("Invalid glob pattern: {}", filter, e);
        return false;
      }
    } else {
      return filePath.equals(filter) || filePath.endsWith("/" + filter);
    }
  }

  private String getFilePath(DiffEntry diff) {
    return switch (diff.getChangeType()) {
      case ADD, MODIFY, COPY, RENAME -> diff.getNewPath();
      case DELETE -> diff.getOldPath();
      default -> diff.getNewPath();
    };
  }

  private AbstractTreeIterator getTreeIterator(Repository repository, RevCommit commit) throws IOException {
    var tree = commit.getTree();
    var treeParser = new CanonicalTreeParser();
    treeParser.reset(repository.newObjectReader(), tree);
    return treeParser;
  }

  private AbstractTreeIterator getEmptyTreeIterator(Repository repository) throws IOException {
    return new EmptyTreeIterator();
  }
}