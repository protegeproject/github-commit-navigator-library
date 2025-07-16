package com.github.navigator.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CommitMetadataTest {

  @Test
  void testJsonSerialization() throws Exception {
    var mapper = new ObjectMapper();
    mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    
    var original = CommitMetadata.create(
        "abc123",
        "testuser",
        LocalDateTime.of(2023, 1, 1, 12, 0, 0),
        "Test commit message"
    );
    
    var json = mapper.writeValueAsString(original);
    System.out.println("JSON: " + json);
    
    var deserialized = mapper.readValue(json, CommitMetadata.class);
    
    assertEquals(original.getCommitHash(), deserialized.getCommitHash());
    assertEquals(original.getCommitterUsername(), deserialized.getCommitterUsername());
    assertEquals(original.getCommitDate(), deserialized.getCommitDate());
    assertEquals(original.getCommitMessage(), deserialized.getCommitMessage());
  }
    
  @Test
  void testEqualsAndHashCode() {
    var commit1 = CommitMetadata.create(
        "abc123",
        "testuser",
        LocalDateTime.of(2023, 1, 1, 12, 0, 0),
        "Test commit message"
    );
    
    var commit2 = CommitMetadata.create(
        "abc123",
        "testuser",
        LocalDateTime.of(2023, 1, 1, 12, 0, 0),
        "Test commit message"
    );
    
    assertEquals(commit1, commit2);
    assertEquals(commit1.hashCode(), commit2.hashCode());
  }
}