package edu.stanford.protege.commitnavigator.exceptions;

/**
 * Exception thrown when repository operations fail.
 * 
 * <p>This exception is thrown when there are issues with Git repository operations
 * such as cloning, fetching, commit navigation, file system access, or when
 * the repository is in an invalid state.</p>
 * 
 * @since 1.0.0
 */
public class RepositoryException extends GitHubNavigatorException {
  /**
   * Constructs a new RepositoryException with the specified detail message.
   * 
   * @param message the detail message explaining the repository operation failure
   */
  public RepositoryException(String message) {
    super(message);
  }

  /**
   * Constructs a new RepositoryException with the specified detail message and cause.
   * 
   * @param message the detail message explaining the repository operation failure
   * @param cause the underlying cause of the repository operation failure
   */
  public RepositoryException(String message, Throwable cause) {
    super(message, cause);
  }
}