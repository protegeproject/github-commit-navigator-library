package edu.stanford.protege.commitnavigator.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Immutable record representing metadata for a Git commit.
 *
 * <p>This record encapsulates essential information about a commit including its hash, author
 * details, timestamp, and message. It supports JSON serialization/deserialization and provides
 * validation to ensure all required fields are non-null.
 *
 * <p>The commit timestamp is stored as an {@link Instant} to preserve exact moment in time across
 * different time zones, ensuring timeline consistency regardless of where the code runs.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * var metadata = CommitMetadata.create(
 *     "abc123def456",
 *     "johndoe",
 *     Instant.now(),
 *     "Fix critical bug in authentication"
 * );
 *
 * System.out.println("Commit: " + metadata.getCommitHash());
 * System.out.println("Author: " + metadata.getCommitterUsername());
 * System.out.println("Date: " + metadata.getCommitDate());
 * System.out.println("Message: " + metadata.getCommitMessage());
 * }</pre>
 *
 * @param commitHash the full SHA-1 hash of the commit
 * @param committerUsername the username of the person who committed the changes
 * @param commitDate the exact moment when the commit was made (timezone-independent)
 * @param commitMessage the commit message describing the changes
 * @since 1.0.0
 */
public record CommitMetadata(
    @JsonProperty("commitHash") String commitHash,
    @JsonProperty("committerUsername") String committerUsername,
    @JsonProperty("committerEmail") String committerEmail,
    @JsonProperty("commitDate") @JsonFormat(shape = JsonFormat.Shape.STRING) Instant commitDate,
    @JsonProperty("commitMessage") String commitMessage) {

  public CommitMetadata {
    Objects.requireNonNull(commitHash, "Commit hash cannot be null");
    Objects.requireNonNull(committerUsername, "Committer username cannot be null");
    Objects.requireNonNull(committerEmail, "Committer email cannot be null");
    Objects.requireNonNull(commitDate, "Commit date cannot be null");
    Objects.requireNonNull(commitMessage, "Commit message cannot be null");
  }

  /**
   * Factory method to create a new {@link CommitMetadata} instance.
   *
   * <p>This method provides a convenient way to create commit metadata instances with proper
   * validation of all required fields.
   *
   * @param commitHash the full SHA-1 hash of the commit
   * @param committerUsername the username of the person who committed the changes
   * @param committerEmail the email of the person who committed the changes
   * @param commitDate the exact moment when the commit was made (timezone-independent)
   * @param commitMessage the commit message describing the changes
   * @return a new {@link CommitMetadata} instance
   * @throws NullPointerException if any parameter is null
   */
  public static CommitMetadata create(
      String commitHash,
      String committerUsername,
      String committerEmail,
      Instant commitDate,
      String commitMessage) {
    return new CommitMetadata(
        commitHash, committerUsername, committerEmail, commitDate, commitMessage);
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
   * Returns the email of the person who committed the changes.
   *
   * @return the committer email
   */
  public String getCommitterEmail() {
    return committerEmail;
  }

  /**
   * Returns the exact moment when the commit was made.
   *
   * <p>This returns an {@link Instant} representing the precise moment in time when the commit was
   * made, independent of time zone. This ensures timeline consistency when commits are made from
   * different time zones.
   *
   * @return the commit timestamp as an {@link Instant}
   */
  public Instant getCommitDate() {
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

  /**
   * Returns the commit date formatted in the specified time zone.
   *
   * <p>This convenience method allows formatting the commit timestamp in any time zone while
   * preserving the original timeline accuracy.
   *
   * @param zone the time zone to format the date in
   * @return the commit date as a {@link ZonedDateTime} in the specified zone
   */
  @JsonIgnore
  public ZonedDateTime getCommitDateInZone(ZoneId zone) {
    return commitDate.atZone(zone);
  }

  /**
   * Returns the commit date formatted in the system's default time zone.
   *
   * <p>This convenience method formats the commit timestamp in the system's default time zone for
   * display purposes.
   *
   * @return the commit date as a {@link ZonedDateTime} in the system default zone
   */
  @JsonIgnore
  public ZonedDateTime getCommitDateInSystemZone() {
    return getCommitDateInZone(ZoneId.systemDefault());
  }
}
