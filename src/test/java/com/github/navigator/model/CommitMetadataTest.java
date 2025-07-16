package com.github.navigator.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CommitMetadataTest {

    @Test
    void testJsonSerialization() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        
        CommitMetadata original = CommitMetadata.create(
            "abc123",
            "testuser",
            LocalDateTime.of(2023, 1, 1, 12, 0, 0),
            "Test commit message"
        );
        
        // Serialize to JSON
        String json = mapper.writeValueAsString(original);
        System.out.println("JSON: " + json);
        
        // Deserialize from JSON
        CommitMetadata deserialized = mapper.readValue(json, CommitMetadata.class);
        
        // Verify
        assertEquals(original.getCommitHash(), deserialized.getCommitHash());
        assertEquals(original.getCommitterUsername(), deserialized.getCommitterUsername());
        assertEquals(original.getCommitDate(), deserialized.getCommitDate());
        assertEquals(original.getCommitMessage(), deserialized.getCommitMessage());
    }
    
    @Test
    void testEqualsAndHashCode() {
        CommitMetadata commit1 = CommitMetadata.create(
            "abc123",
            "testuser",
            LocalDateTime.of(2023, 1, 1, 12, 0, 0),
            "Test commit message"
        );
        
        CommitMetadata commit2 = CommitMetadata.create(
            "abc123",
            "testuser",
            LocalDateTime.of(2023, 1, 1, 12, 0, 0),
            "Test commit message"
        );
        
        assertEquals(commit1, commit2);
        assertEquals(commit1.hashCode(), commit2.hashCode());
    }
}