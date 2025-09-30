package edu.stanford.protege.commitnavigator;

import edu.stanford.protege.commitnavigator.model.BranchCoordinates;
import edu.stanford.protege.commitnavigator.utils.AuthenticationManager;
import edu.stanford.protege.commitnavigator.utils.impl.AuthenticationManagerImpl;
import java.util.Objects;

/**
 * Factory class for creating pre-configured {@link GitHubRepositoryBuilder} instances.
 *
 * <p>This factory implements the Factory-Builder pattern by providing methods that create builders
 * with required dependencies already injected and repository coordinates pre-configured. The
 * factory separates "what to build" (public vs authenticated repositories) from "how to build"
 * (specific configuration options available through the builder).
 *
 * <p>Usage examples:
 *
 * <pre>{@code
 * // Public repository
 * var coordinate = RepositoryCoordinates.create("owner", "repo");
 * var repository = GitHubRepositoryBuilderFactory.create(coordinate)
 *     .localWorkingDirectory("/tmp/repo")
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public class GitHubRepositoryBuilderFactory {

  /**
   * Creates a pre-configured builder for accessing public repositories.
   *
   * <p>The returned builder includes default implementations of {@link AuthenticationManager} and
   * the supplied repository coordinates.
   *
   * @param branchCoordinates the repository coordinate containing owner, repo, and branch
   * @return a pre-configured {@link GitHubRepositoryBuilder} for public repository access
   * @throws NullPointerException if repositoryCoordinates is null
   */
  public static GitHubRepositoryBuilder create(BranchCoordinates branchCoordinates) {
    Objects.requireNonNull(branchCoordinates, "Repository coordinate cannot be null");

    var authManager = new AuthenticationManagerImpl();

    return new GitHubRepositoryBuilder(branchCoordinates, authManager);
  }
}
