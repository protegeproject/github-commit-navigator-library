package edu.stanford.protege.webprotege.exceptions;

/**
 * Base exception class for GitHub repository navigator operations.
 * 
 * <p>This exception serves as the parent class for all specific exceptions
 * that can occur during repository navigation, authentication, and Git operations.
 * It provides a consistent exception hierarchy for error handling.</p>
 * 
 * @since 1.0.0
 */
public class GitHubNavigatorException extends Exception {
  /**
   * Constructs a new GitHubNavigatorException with the specified detail message.
   * 
   * @param message the detail message explaining the exception
   */
  public GitHubNavigatorException(String message) {
    super(message);
  }

  /**
   * Constructs a new GitHubNavigatorException with the specified detail message and cause.
   * 
   * @param message the detail message explaining the exception
   * @param cause the cause of the exception
   */
  public GitHubNavigatorException(String message, Throwable cause) {
    super(message, cause);
  }
}