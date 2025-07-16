package edu.stanford.protege.webprotege.exceptions;

public class GitHubNavigatorException extends Exception {
  public GitHubNavigatorException(String message) {
    super(message);
  }

  public GitHubNavigatorException(String message, Throwable cause) {
    super(message, cause);
  }
}