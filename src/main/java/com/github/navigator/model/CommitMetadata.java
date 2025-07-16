package com.github.navigator.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents metadata about a Git commit including hash, committer information, date, and message.
 */
public class CommitMetadata {
    
    @JsonProperty("commitHash")
    private final String commitHash;
    
    @JsonProperty("committerUsername")
    private final String committerUsername;
    
    @JsonProperty("commitDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime commitDate;
    
    @JsonProperty("commitMessage")
    private final String commitMessage;
    
    @JsonCreator
    public CommitMetadata(
            @JsonProperty("commitHash") String commitHash,
            @JsonProperty("committerUsername") String committerUsername,
            @JsonProperty("commitDate") LocalDateTime commitDate,
            @JsonProperty("commitMessage") String commitMessage) {
        this.commitHash = commitHash;
        this.committerUsername = committerUsername;
        this.commitDate = commitDate;
        this.commitMessage = commitMessage;
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
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommitMetadata that = (CommitMetadata) o;
        return Objects.equals(commitHash, that.commitHash) &&
               Objects.equals(committerUsername, that.committerUsername) &&
               Objects.equals(commitDate, that.commitDate) &&
               Objects.equals(commitMessage, that.commitMessage);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(commitHash, committerUsername, commitDate, commitMessage);
    }
    
    @Override
    public String toString() {
        return "CommitMetadata{" +
               "commitHash='" + commitHash + '\'' +
               ", committerUsername='" + committerUsername + '\'' +
               ", commitDate=" + commitDate +
               ", commitMessage='" + commitMessage + '\'' +
               '}';
    }
}