package edu.stanford.protege.webprotege.exceptions;

public class RepositoryException extends GitHubNavigatorException {
  public RepositoryException(String message) {
    super(message);
  }

  public RepositoryException(String message, Throwable cause) {
    super(message, cause);
  }
}