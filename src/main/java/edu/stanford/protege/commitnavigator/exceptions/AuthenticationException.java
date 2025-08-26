package edu.stanford.protege.commitnavigator.exceptions;

/**
 * Exception thrown when authentication with GitHub fails.
 *
 * <p>This exception is thrown when there are issues with authentication credentials, invalid
 * tokens, network connectivity problems during authentication, or when the authentication method is
 * not supported.
 *
 * @since 1.0.0
 */
public class AuthenticationException extends GitHubNavigatorException {
  /**
   * Constructs a new AuthenticationException with the specified detail message.
   *
   * @param message the detail message explaining the authentication failure
   */
  public AuthenticationException(String message) {
    super(message);
  }

  /**
   * Constructs a new AuthenticationException with the specified detail message and cause.
   *
   * @param message the detail message explaining the authentication failure
   * @param cause the underlying cause of the authentication failure
   */
  public AuthenticationException(String message, Throwable cause) {
    super(message, cause);
  }
}
