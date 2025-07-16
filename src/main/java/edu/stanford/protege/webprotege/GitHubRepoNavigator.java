package edu.stanford.protege.webprotege;

import edu.stanford.protege.webprotege.config.NavigatorConfig;
import edu.stanford.protege.webprotege.exceptions.GitHubNavigatorException;
import edu.stanford.protege.webprotege.services.CommitNavigator;

public interface GitHubRepoNavigator {
  void initialize() throws GitHubNavigatorException;
  CommitNavigator getCommitNavigator() throws GitHubNavigatorException;
  void fetchLatestChanges() throws GitHubNavigatorException;
  void close() throws GitHubNavigatorException;
  NavigatorConfig getConfig();
}