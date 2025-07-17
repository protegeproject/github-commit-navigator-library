package edu.stanford.protege.webprotege.services;

import edu.stanford.protege.webprotege.exceptions.RepositoryException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import java.util.List;

/**
 * Service interface for detecting file changes in Git commits.
 * 
 * <p>This interface provides methods to analyze commits and determine which files
 * were modified, added, or deleted. It supports file filtering based on glob patterns
 * to focus on specific types of files or directories.</p>
 * 
 * @since 1.0.0
 */
public interface FileChangeDetector {
  /**
   * Checks if the specified commit has file changes that match the provided filters.
   * 
   * @param repository the Git repository to analyze
   * @param commit the commit to check for file changes
   * @param fileFilters the list of file filter patterns (glob patterns supported)
   * @return true if the commit has matching file changes, false otherwise
   * @throws RepositoryException if an error occurs while analyzing the commit
   */
  boolean hasFileChanges(Repository repository, RevCommit commit, List<String> fileFilters) throws RepositoryException;
  /**
   * Retrieves all files that were changed in the specified commit.
   * 
   * @param repository the Git repository to analyze
   * @param commit the commit to analyze for file changes
   * @return a list of file paths that were modified, added, or deleted in the commit
   * @throws RepositoryException if an error occurs while analyzing the commit
   */
  List<String> getChangedFiles(Repository repository, RevCommit commit) throws RepositoryException;
}