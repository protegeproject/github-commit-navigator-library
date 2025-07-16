package com.github.navigator;

import com.github.navigator.config.NavigatorConfig;
import com.github.navigator.exceptions.GitHubNavigatorException;
import com.github.navigator.services.CommitNavigator;

public interface GitHubRepoNavigator {
  void initialize() throws GitHubNavigatorException;
  CommitNavigator getCommitNavigator() throws GitHubNavigatorException;
  void fetchLatestChanges() throws GitHubNavigatorException;
  void close() throws GitHubNavigatorException;
  NavigatorConfig getConfig();
}