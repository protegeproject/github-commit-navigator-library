package com.github.navigator.services.impl;

import com.github.navigator.config.AuthenticationConfig;
import com.github.navigator.exceptions.AuthenticationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationManagerImplTest {

    private AuthenticationManagerImpl authManager;

    @BeforeEach
    void setUp() {
        authManager = new AuthenticationManagerImpl();
    }

    @Test
    void testValidateAuthentication_ValidPersonalAccessToken() throws AuthenticationException {
        AuthenticationConfig config = AuthenticationConfig
            .personalAccessToken("valid-token")
            .build();

        assertDoesNotThrow(() -> {
            authManager.getCredentialsProvider(config);
        });
    }

    @Test
    void testValidateAuthentication_NullConfig() {
        // Null config should not throw an exception since anonymous access is allowed
        assertDoesNotThrow(() -> {
            authManager.validateAuthentication(null);
        });
    }

    @Test
    void testValidateAuthentication_EmptyToken() {
        AuthenticationConfig config = AuthenticationConfig
            .personalAccessToken("")
            .build();

        assertThrows(AuthenticationException.class, () -> {
            authManager.validateAuthentication(config);
        });
    }

    @Test
    void testValidateAuthentication_NullToken() {
        AuthenticationConfig config = AuthenticationConfig
            .personalAccessToken(null)
            .build();

        assertThrows(AuthenticationException.class, () -> {
            authManager.validateAuthentication(config);
        });
    }

    @Test
    void testValidateAuthentication_UsernamePassword() throws AuthenticationException {
        AuthenticationConfig config = AuthenticationConfig
            .usernamePassword("testuser", "testpass")
            .build();

        assertDoesNotThrow(() -> {
            authManager.getCredentialsProvider(config);
        });
    }

    @Test
    void testValidateAuthentication_EmptyUsername() {
        AuthenticationConfig config = AuthenticationConfig
            .usernamePassword("", "testpass")
            .build();

        assertThrows(AuthenticationException.class, () -> {
            authManager.validateAuthentication(config);
        });
    }

    @Test
    void testValidateAuthentication_EmptyPassword() {
        AuthenticationConfig config = AuthenticationConfig
            .usernamePassword("testuser", "")
            .build();

        assertThrows(AuthenticationException.class, () -> {
            authManager.validateAuthentication(config);
        });
    }

    @Test
    void testGetCredentialsProvider_PersonalAccessToken() throws AuthenticationException {
        AuthenticationConfig config = AuthenticationConfig
            .personalAccessToken("test-token")
            .build();

        var provider = authManager.getCredentialsProvider(config);
        assertNotNull(provider);
    }

    @Test
    void testGetCredentialsProvider_OAuth() throws AuthenticationException {
        AuthenticationConfig config = AuthenticationConfig
            .oauth("oauth-token")
            .build();

        var provider = authManager.getCredentialsProvider(config);
        assertNotNull(provider);
    }

    @Test
    void testGetCredentialsProvider_SshKey() {
        AuthenticationConfig config = AuthenticationConfig
            .sshKey("/path/to/key")
            .build();

        assertThrows(AuthenticationException.class, () -> {
            authManager.getCredentialsProvider(config);
        });
    }
    
    @Test
    void testGetCredentialsProvider_NullConfig() throws AuthenticationException {
        // Null config should return null provider for anonymous access
        var provider = authManager.getCredentialsProvider(null);
        assertNull(provider);
    }
    
    @Test
    void testAuthenticateGitHub_NullConfig() throws AuthenticationException {
        // Null config should connect to GitHub anonymously
        var github = authManager.authenticateGitHub(null);
        assertNotNull(github);
    }
}