package edu.stanford.protege.commitnavigator.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CommitMetadataSerializationTest {

  @Test
  void testRecordSerialization() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    CommitMetadata original = new CommitMetadata(
      "def456",
      "recorduser",
      LocalDateTime.of(2023, 6, 15, 10, 30, 0),
      "Record pattern commit"
    );

    // Serialize to JSON
    String json = mapper.writeValueAsString(original);
    System.out.println("Record JSON: " + json);

    // Deserialize from JSON
    CommitMetadata deserialized = mapper.readValue(json, CommitMetadata.class);

    // Verify using record accessors
    assertEquals(original.commitHash(), deserialized.commitHash());
    assertEquals(original.committerUsername(), deserialized.committerUsername());
    assertEquals(original.commitDate(), deserialized.commitDate());
    assertEquals(original.commitMessage(), deserialized.commitMessage());

    // Also test backward compatibility getters
    assertEquals(original.getCommitHash(), deserialized.getCommitHash());
    assertEquals(original.getCommitterUsername(), deserialized.getCommitterUsername());
    assertEquals(original.getCommitDate(), deserialized.getCommitDate());
    assertEquals(original.getCommitMessage(), deserialized.getCommitMessage());
  }

  @Test
  void testRecordStringRepresentation() {
    CommitMetadata commit = CommitMetadata.create(
      "xyz789",
      "recorduser",
      LocalDateTime.of(2023, 12, 1, 14, 45, 30),
      "Record test commit"
    );

    String toString = commit.toString();
    System.out.println("Record toString: " + toString);

    // Records provide meaningful toString
    assertTrue(toString.contains("xyz789"));
    assertTrue(toString.contains("recorduser"));
    assertTrue(toString.contains("Record test commit"));
  }
}