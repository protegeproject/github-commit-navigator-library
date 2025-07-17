package edu.stanford.protege.commitnavigator.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Immutable record representing metadata for a Git commit.
 * 
 * <p>This record encapsulates essential information about a commit including its hash,
 * author details, timestamp, and message. It supports JSON serialization/deserialization
 * and provides validation to ensure all required fields are non-null.</p>
 * 
 * <p>Usage example:</p>
 * <pre>
 * {@code
 * var metadata = CommitMetadata.create(
 *     "abc123def456",
 *     "johndoe",
 *     LocalDateTime.now(),
 *     "Fix critical bug in authentication"
 * );
 * 
 * System.out.println("Commit: " + metadata.getCommitHash());
 * System.out.println("Author: " + metadata.getCommitterUsername());
 * System.out.println("Date: " + metadata.getCommitDate());
 * System.out.println("Message: " + metadata.getCommitMessage());
 * }
 * </pre>
 * 
 * @param commitHash the full SHA-1 hash of the commit
 * @param committerUsername the username of the person who committed the changes
 * @param commitDate the date and time when the commit was made
 * @param commitMessage the commit message describing the changes
 * 
 * @since 1.0.0
 */
public record CommitMetadata(
  @JsonProperty("commitHash") 
  String commitHash,
  
  @JsonProperty("committerUsername") 
  String committerUsername,
  
  @JsonProperty("commitDate")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
  LocalDateTime commitDate,
  
  @JsonProperty("commitMessage") 
  String commitMessage
) {
  
  public CommitMetadata {
    Objects.requireNonNull(commitHash, "Commit hash cannot be null");
    Objects.requireNonNull(committerUsername, "Committer username cannot be null");
    Objects.requireNonNull(commitDate, "Commit date cannot be null");
    Objects.requireNonNull(commitMessage, "Commit message cannot be null");
  }
  
  /**
   * Factory method to create a new {@link CommitMetadata} instance.
   * 
   * <p>This method provides a convenient way to create commit metadata instances
   * with proper validation of all required fields.</p>
   * 
   * @param commitHash the full SHA-1 hash of the commit
   * @param committerUsername the username of the person who committed the changes
   * @param commitDate the date and time when the commit was made
   * @param commitMessage the commit message describing the changes
   * @return a new {@link CommitMetadata} instance
   * @throws NullPointerException if any parameter is null
   */
  public static CommitMetadata create(String commitHash, String committerUsername, LocalDateTime commitDate, String commitMessage) {
    return new CommitMetadata(commitHash, committerUsername, commitDate, commitMessage);
  }
  
  /**
   * Returns the full SHA-1 hash of the commit.
   * 
   * @return the commit hash string
   */
  public String getCommitHash() {
    return commitHash;
  }
  
  /**
   * Returns the username of the person who committed the changes.
   * 
   * @return the committer username
   */
  public String getCommitterUsername() {
    return committerUsername;
  }
  
  /**
   * Returns the date and time when the commit was made.
   * 
   * @return the commit date and time
   */
  public LocalDateTime getCommitDate() {
    return commitDate;
  }
  
  /**
   * Returns the commit message describing the changes.
   * 
   * @return the commit message
   */
  public String getCommitMessage() {
    return commitMessage;
  }
}