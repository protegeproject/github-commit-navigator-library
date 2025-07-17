package edu.stanford.protege.webprotege;

import edu.stanford.protege.webprotege.config.NavigatorConfig;
import edu.stanford.protege.webprotege.impl.GitHubRepoNavigatorImpl;
import edu.stanford.protege.webprotege.services.AuthenticationManager;
import edu.stanford.protege.webprotege.services.FileChangeDetector;
import edu.stanford.protege.webprotege.services.impl.AuthenticationManagerImpl;
import edu.stanford.protege.webprotege.services.impl.FileChangeDetectorImpl;

/**
 * Factory class for creating {@link GitHubRepoNavigator} instances with dependency injection.
 * 
 * <p>This factory provides static methods to create navigator instances with default
 * or custom service implementations. It serves as the central point for instantiating
 * the navigator with appropriate dependencies.</p>
 * 
 * @since 1.0.0
 */
public class GitHubRepoNavigatorFactory {

  /**
   * Creates a {@link GitHubRepoNavigator} instance with default service implementations.
   * 
   * <p>This method uses the default implementations of {@link AuthenticationManager}
   * and {@link FileChangeDetector} services.</p>
   * 
   * @param config the navigation configuration
   * @return a configured {@link GitHubRepoNavigator} instance
   * @throws NullPointerException if config is null
   */
  public static GitHubRepoNavigator create(NavigatorConfig config) {
    var authManager = new AuthenticationManagerImpl();
    var fileChangeDetector = new FileChangeDetectorImpl();
    return new GitHubRepoNavigatorImpl(config, authManager, fileChangeDetector);
  }

  /**
   * Creates a {@link GitHubRepoNavigator} instance with custom service implementations.
   * 
   * <p>This method allows for dependency injection of custom service implementations,
   * useful for testing or when alternative implementations are required.</p>
   * 
   * @param config the navigation configuration
   * @param authManager the authentication manager implementation
   * @param fileChangeDetector the file change detector implementation
   * @return a configured {@link GitHubRepoNavigator} instance
   * @throws NullPointerException if any parameter is null
   */
  public static GitHubRepoNavigator create(NavigatorConfig config,
                                           AuthenticationManager authManager,
                                           FileChangeDetector fileChangeDetector) {
    return new GitHubRepoNavigatorImpl(config, authManager, fileChangeDetector);
  }
}