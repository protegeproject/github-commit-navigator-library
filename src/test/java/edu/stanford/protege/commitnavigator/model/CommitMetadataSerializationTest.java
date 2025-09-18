package edu.stanford.protege.commitnavigator.model;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CommitMetadataSerializationTest {

  private static final Logger logger =
      LoggerFactory.getLogger(CommitMetadataSerializationTest.class);

  @Test
  void testRecordSerialization() throws Exception {
    var mapper = new ObjectMapper();
    mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    var original =
        new CommitMetadata(
            "def456",
            "recorduser",
            "recorduser@example.com",
            Instant.parse("2023-06-15T10:30:00Z"),
            "Record pattern commit",
            List.of("src/main/Test.java", "README.md"));

    // Serialize to JSON
    var json = mapper.writeValueAsString(original);
    logger.debug("Record JSON: {}", json);

    // Deserialize from JSON
    var deserialized = mapper.readValue(json, CommitMetadata.class);

    // Verify using record accessors
    assertEquals(original.commitHash(), deserialized.commitHash());
    assertEquals(original.committerUsername(), deserialized.committerUsername());
    assertEquals(original.commitDate(), deserialized.commitDate());
    assertEquals(original.commitMessage(), deserialized.commitMessage());
    assertEquals(original.changedFiles(), deserialized.changedFiles());

    // Also test backward compatibility getters
    assertEquals(original.getCommitHash(), deserialized.getCommitHash());
    assertEquals(original.getCommitterUsername(), deserialized.getCommitterUsername());
    assertEquals(original.getCommitDate(), deserialized.getCommitDate());
    assertEquals(original.getCommitMessage(), deserialized.getCommitMessage());
    assertEquals(original.getChangedFiles(), deserialized.getChangedFiles());
  }

  @Test
  void testRecordStringRepresentation() {
    var commit =
        CommitMetadata.create(
            "xyz789",
            "recorduser",
            "recorduser@example.com",
            Instant.parse("2023-12-01T14:45:30Z"),
            "Record test commit",
            List.of("pom.xml", "src/main/App.java"));

    var toString = commit.toString();
    logger.debug("Record toString: {}", toString);

    // Records provide meaningful toString
    assertTrue(toString.contains("xyz789"));
    assertTrue(toString.contains("recorduser"));
    assertTrue(toString.contains("Record test commit"));
  }
}
