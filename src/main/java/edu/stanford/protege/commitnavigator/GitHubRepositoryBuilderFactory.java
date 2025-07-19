package edu.stanford.protege.commitnavigator;

import edu.stanford.protege.commitnavigator.model.RepositoryCoordinate;
import edu.stanford.protege.commitnavigator.services.AuthenticationManager;
import edu.stanford.protege.commitnavigator.services.FileChangeDetector;
import edu.stanford.protege.commitnavigator.services.impl.AuthenticationManagerImpl;
import edu.stanford.protege.commitnavigator.services.impl.FileChangeDetectorImpl;

import java.util.Objects;

/**
 * Factory class for creating pre-configured {@link GitHubRepositoryBuilder} instances.
 * 
 * <p>This factory implements the Factory-Builder pattern by providing methods that create
 * builders with required dependencies already injected and repository coordinates pre-configured.
 * The factory separates "what to build" (public vs authenticated repositories) from "how to build"
 * (specific configuration options available through the builder).</p>
 * 
 * <p>Usage examples:</p>
 * <pre>
 * {@code
 * // Public repository
 * var coordinate = RepositoryCoordinate.create("owner", "repo");
 * var repository = GitHubRepositoryBuilderFactory.create(coordinate)
 *     .localCloneDirectory("/tmp/repo")
 *     .build();
 * }
 * </pre>
 * 
 * @since 1.0.0
 */
public class GitHubRepositoryBuilderFactory {

  /**
   * Creates a pre-configured builder for accessing public repositories.
   * 
   * <p>The returned builder includes default implementations of {@link AuthenticationManager}
   * and {@link FileChangeDetector}, with the repository URL and branch pre-configured based
   * on the provided coordinate.</p>
   * 
   * @param repositoryCoordinate the repository coordinate containing owner, repo, and branch
   * @return a pre-configured {@link GitHubRepositoryBuilder} for public repository access
   * @throws NullPointerException if repositoryCoordinate is null
   */
  public static GitHubRepositoryBuilder create(RepositoryCoordinate repositoryCoordinate) {
    Objects.requireNonNull(repositoryCoordinate, "Repository coordinate cannot be null");

    var authManager = new AuthenticationManagerImpl();
    var fileChangeDetector = new FileChangeDetectorImpl();
    
    return new GitHubRepositoryBuilder(repositoryCoordinate, authManager, fileChangeDetector);
  }
}