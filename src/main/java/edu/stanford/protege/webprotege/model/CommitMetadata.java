package edu.stanford.protege.webprotege.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Objects;

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
  
  public static CommitMetadata create(String commitHash, String committerUsername, LocalDateTime commitDate, String commitMessage) {
    return new CommitMetadata(commitHash, committerUsername, commitDate, commitMessage);
  }
  
  public String getCommitHash() {
    return commitHash;
  }
  
  public String getCommitterUsername() {
    return committerUsername;
  }
  
  public LocalDateTime getCommitDate() {
    return commitDate;
  }
  
  public String getCommitMessage() {
    return commitMessage;
  }
}