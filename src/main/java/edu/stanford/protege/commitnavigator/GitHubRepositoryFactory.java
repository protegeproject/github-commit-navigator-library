package edu.stanford.protege.commitnavigator;

import edu.stanford.protege.commitnavigator.config.RepositoryConfig;
import edu.stanford.protege.commitnavigator.impl.GitHubRepositoryImpl;
import edu.stanford.protege.commitnavigator.services.AuthenticationManager;
import edu.stanford.protege.commitnavigator.services.FileChangeDetector;
import edu.stanford.protege.commitnavigator.services.impl.AuthenticationManagerImpl;
import edu.stanford.protege.commitnavigator.services.impl.FileChangeDetectorImpl;

/**
 * Factory class for creating {@link GitHubRepository} instances with dependency injection.
 * 
 * <p>This factory provides static methods to create navigator instances with default
 * or custom service implementations. It serves as the central point for instantiating
 * the navigator with appropriate dependencies.</p>
 * 
 * @since 1.0.0
 */
public class GitHubRepositoryFactory {

  /**
   * Creates a {@link GitHubRepository} instance with default service implementations.
   * 
   * <p>This method uses the default implementations of {@link AuthenticationManager}
   * and {@link FileChangeDetector} services.</p>
   * 
   * @param config the navigation configuration
   * @return a configured {@link GitHubRepository} instance
   * @throws NullPointerException if config is null
   */
  public static GitHubRepository create(RepositoryConfig config) {
    var authManager = new AuthenticationManagerImpl();
    var fileChangeDetector = new FileChangeDetectorImpl();
    return new GitHubRepositoryImpl(config, authManager, fileChangeDetector);
  }

  /**
   * Creates a {@link GitHubRepository} instance with custom service implementations.
   * 
   * <p>This method allows for dependency injection of custom service implementations,
   * useful for testing or when alternative implementations are required.</p>
   * 
   * @param config the navigation configuration
   * @param authManager the authentication manager implementation
   * @param fileChangeDetector the file change detector implementation
   * @return a configured {@link GitHubRepository} instance
   * @throws NullPointerException if any parameter is null
   */
  public static GitHubRepository create(RepositoryConfig config,
                                        AuthenticationManager authManager,
                                        FileChangeDetector fileChangeDetector) {
    return new GitHubRepositoryImpl(config, authManager, fileChangeDetector);
  }
}