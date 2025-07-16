package edu.stanford.protege.webprotege;

import edu.stanford.protege.webprotege.config.NavigatorConfig;
import edu.stanford.protege.webprotege.impl.GitHubRepoNavigatorImpl;
import edu.stanford.protege.webprotege.services.AuthenticationManager;
import edu.stanford.protege.webprotege.services.FileChangeDetector;
import edu.stanford.protege.webprotege.services.impl.AuthenticationManagerImpl;
import edu.stanford.protege.webprotege.services.impl.FileChangeDetectorImpl;

public class GitHubRepoNavigatorFactory {

  public static GitHubRepoNavigator create(NavigatorConfig config) {
    var authManager = new AuthenticationManagerImpl();
    var fileChangeDetector = new FileChangeDetectorImpl();
    return new GitHubRepoNavigatorImpl(config, authManager, fileChangeDetector);
  }

  public static GitHubRepoNavigator create(NavigatorConfig config,
                                           AuthenticationManager authManager,
                                           FileChangeDetector fileChangeDetector) {
    return new GitHubRepoNavigatorImpl(config, authManager, fileChangeDetector);
  }
}