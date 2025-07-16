package edu.stanford.protege.webprotege.exceptions;

public class AuthenticationException extends GitHubNavigatorException {
  public AuthenticationException(String message) {
    super(message);
  }

  public AuthenticationException(String message, Throwable cause) {
    super(message, cause);
  }
}