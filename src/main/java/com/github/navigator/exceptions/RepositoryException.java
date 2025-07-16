package com.github.navigator.exceptions;

public class RepositoryException extends GitHubNavigatorException {
  public RepositoryException(String message) {
    super(message);
  }

  public RepositoryException(String message, Throwable cause) {
    super(message, cause);
  }
}