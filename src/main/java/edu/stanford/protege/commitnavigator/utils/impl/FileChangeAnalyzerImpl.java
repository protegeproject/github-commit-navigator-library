package edu.stanford.protege.commitnavigator.utils.impl;

import edu.stanford.protege.commitnavigator.exceptions.RepositoryException;
import edu.stanford.protege.commitnavigator.utils.FileChangeAnalyzer;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Singleton;
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

/**
 * Default implementation of {@link FileChangeAnalyzer} that analyzes Git commits to detect file
 * changes and apply filtering based on file patterns.
 *
 * <p>This implementation uses JGit's diff functionality to analyze changes between commits and
 * their parent commits. It supports glob pattern matching for file filtering and handles various
 * types of file changes including additions, modifications, deletions, copies, and renames.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Efficient diff analysis using JGit's DiffFormatter
 *   <li>Glob pattern matching for file filtering
 *   <li>Support for exact path matching and pattern-based filtering
 *   <li>Handling of initial commits (commits without parents)
 *   <li>Proper resource management with try-with-resources
 * </ul>
 *
 * <p>The class is marked as {@link Singleton} to ensure a single instance is used throughout the
 * application lifecycle.
 *
 * @since 1.0.0
 */
@Singleton
public class FileChangeAnalyzerImpl implements FileChangeAnalyzer {
  private static final Logger logger = LoggerFactory.getLogger(FileChangeAnalyzerImpl.class);

  /**
   * Checks if the specified commit has file changes that match the provided filters.
   *
   * <p>This method analyzes the commit's diff against its parent commits to determine which files
   * were changed, then applies the provided file filters to check for matches. If no filters are
   * provided, the method returns true (all commits match).
   *
   * <p>The filtering supports both exact path matching and glob pattern matching:
   *
   * <ul>
   *   <li>Exact paths: "src/main/java/Example.java"
   *   <li>Glob patterns: "*.java", "**\/*.md", "src\/**\/*.xml"
   * </ul>
   *
   * @param repository the Git repository to analyze
   * @param commit the commit to check for matching file changes
   * @param fileFilters the list of file filter patterns, or null/empty to match all commits
   * @return true if the commit has file changes matching the filters, false otherwise
   * @throws RepositoryException if an error occurs while analyzing the commit
   * @throws NullPointerException if repository or commit is null
   */
  @Override
  public boolean hasFileChanges(Repository repository, RevCommit commit, List<String> fileFilters)
      throws RepositoryException {
    Objects.requireNonNull(repository, "Repository cannot be null");
    Objects.requireNonNull(commit, "Commit cannot be null");

    if (fileFilters == null || fileFilters.isEmpty()) {
      return true;
    }

    try {
      var changedFiles = getChangedFiles(repository, commit);
      return changedFiles.stream().anyMatch(file -> matchesAnyFilter(file, fileFilters));
    } catch (Exception e) {
      throw new RepositoryException(
          "Failed to check file changes in commit " + commit.getName(), e);
    }
  }

  /**
   * Retrieves all files that were changed in the specified commit.
   *
   * <p>This method analyzes the commit's diff against its parent commits to determine which files
   * were added, modified, deleted, copied, or renamed. For commits without parents (initial
   * commits), it compares against an empty tree to show all added files.
   *
   * <p>The method handles the following change types:
   *
   * <ul>
   *   <li>ADD - Files added in the commit
   *   <li>MODIFY - Files modified in the commit
   *   <li>DELETE - Files deleted in the commit
   *   <li>COPY - Files copied in the commit
   *   <li>RENAME - Files renamed in the commit
   * </ul>
   *
   * @param repository the Git repository to analyze
   * @param commit the commit to analyze for file changes
   * @return a list of file paths that were changed in the commit
   * @throws RepositoryException if an error occurs while analyzing the commit
   * @throws NullPointerException if repository or commit is null
   */
  @Override
  public List<String> getChangedFiles(Repository repository, RevCommit commit)
      throws RepositoryException {
    Objects.requireNonNull(repository, "Repository cannot be null");
    Objects.requireNonNull(commit, "Commit cannot be null");

    logger.debug("Getting changed files for commit: {}", commit.getName());

    try (var revWalk = new RevWalk(repository);
        var diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {

      diffFormatter.setRepository(repository);

      var parents = commit.getParents();
      var diffs = new ArrayList<DiffEntry>();

      if (parents.length == 0) {
        diffs.addAll(
            diffFormatter.scan(
                getEmptyTreeIterator(repository), getTreeIterator(repository, commit)));
      } else {
        for (var parent : parents) {
          revWalk.parseCommit(parent);
          var parentDiffs =
              diffFormatter.scan(
                  getTreeIterator(repository, parent), getTreeIterator(repository, commit));
          diffs.addAll(parentDiffs);
        }
      }

      return diffs.stream()
          .map(this::getFilePath)
          .filter(
              path ->
                  Optional.ofNullable(path).map(String::trim).filter(p -> !p.isEmpty()).isPresent())
          .toList();

    } catch (IOException e) {
      throw new RepositoryException(
          "Failed to get changed files for commit " + commit.getName(), e);
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

  private AbstractTreeIterator getTreeIterator(Repository repository, RevCommit commit)
      throws IOException {
    var tree = commit.getTree();
    var treeParser = new CanonicalTreeParser();
    treeParser.reset(repository.newObjectReader(), tree);
    return treeParser;
  }

  private AbstractTreeIterator getEmptyTreeIterator(Repository repository) throws IOException {
    return new EmptyTreeIterator();
  }
}
