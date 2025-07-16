package com.github.navigator;

import com.github.navigator.config.NavigatorConfig;
import com.github.navigator.impl.GitHubRepoNavigatorImpl;
import com.github.navigator.services.AuthenticationManager;
import com.github.navigator.services.FileChangeDetector;
import com.github.navigator.services.impl.AuthenticationManagerImpl;
import com.github.navigator.services.impl.FileChangeDetectorImpl;

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