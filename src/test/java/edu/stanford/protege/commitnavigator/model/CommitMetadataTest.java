package edu.stanford.protege.commitnavigator.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class CommitMetadataTest {

    private static final Logger logger = LoggerFactory.getLogger(CommitMetadataTest.class);

    @Test
    void testJsonSerialization() throws Exception {
        var mapper = new ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        
        var original = CommitMetadata.create(
                "abc123",
                "testuser",
                Instant.parse("2023-01-01T12:00:00Z"),
                "Test commit message"
        );
        
        var json = mapper.writeValueAsString(original);
        logger.debug("JSON: {}", json);
        
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
                Instant.parse("2023-01-01T12:00:00Z"),
                "Test commit message"
        );
        
        var commit2 = CommitMetadata.create(
                "abc123",
                "testuser",
                Instant.parse("2023-01-01T12:00:00Z"),
                "Test commit message"
        );
        
        assertEquals(commit1, commit2);
        assertEquals(commit1.hashCode(), commit2.hashCode());
    }
}