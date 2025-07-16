package com.github.navigator.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * Represents metadata about a Git commit including hash, committer information, date, and message.
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
    
    public static CommitMetadata create(String commitHash, String committerUsername, LocalDateTime commitDate, String commitMessage) {
        return new CommitMetadata(commitHash, committerUsername, commitDate, commitMessage);
    }
    
    // Convenience getters for backward compatibility
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