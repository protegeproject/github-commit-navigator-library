package edu.stanford.protege.commitnavigator.services.impl;

import edu.stanford.protege.commitnavigator.config.AuthenticationConfig;
import edu.stanford.protege.commitnavigator.exceptions.AuthenticationException;
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
    var config = AuthenticationConfig
        .personalAccessToken("valid-token")
        .build();

    assertDoesNotThrow(() -> {
      authManager.getCredentialsProvider(config);
    });
  }

  @Test
  void testValidateAuthentication_NullConfig() {
    assertDoesNotThrow(() -> {
      authManager.validateAuthentication(null);
    });
  }

  @Test
  void testValidateAuthentication_EmptyToken() {
    var config = AuthenticationConfig
        .personalAccessToken("")
        .build();

    assertThrows(AuthenticationException.class, () -> {
      authManager.validateAuthentication(config);
    });
  }

  @Test
  void testValidateAuthentication_NullToken() {
    var config = AuthenticationConfig
        .personalAccessToken(null)
        .build();

    assertThrows(AuthenticationException.class, () -> {
      authManager.validateAuthentication(config);
    });
  }

  @Test
  void testValidateAuthentication_UsernamePassword() throws AuthenticationException {
    var config = AuthenticationConfig
        .usernamePassword("testuser", "testpass")
        .build();

    assertDoesNotThrow(() -> {
      authManager.getCredentialsProvider(config);
    });
  }

  @Test
  void testValidateAuthentication_EmptyUsername() {
    var config = AuthenticationConfig
        .usernamePassword("", "testpass")
        .build();

    assertThrows(AuthenticationException.class, () -> {
      authManager.validateAuthentication(config);
    });
  }

  @Test
  void testValidateAuthentication_EmptyPassword() {
    var config = AuthenticationConfig
        .usernamePassword("testuser", "")
        .build();

    assertThrows(AuthenticationException.class, () -> {
      authManager.validateAuthentication(config);
    });
  }

  @Test
  void testGetCredentialsProvider_PersonalAccessToken() throws AuthenticationException {
    var config = AuthenticationConfig
        .personalAccessToken("test-token")
        .build();

    var provider = authManager.getCredentialsProvider(config);
    assertNotNull(provider);
  }

  @Test
  void testGetCredentialsProvider_OAuth() throws AuthenticationException {
    var config = AuthenticationConfig
        .oauth("oauth-token")
        .build();

    var provider = authManager.getCredentialsProvider(config);
    assertNotNull(provider);
  }

  @Test
  void testGetCredentialsProvider_SshKey() {
    var config = AuthenticationConfig
        .sshKey("/path/to/key")
        .build();

    assertThrows(AuthenticationException.class, () -> {
      authManager.getCredentialsProvider(config);
    });
  }
    
  @Test
  void testGetCredentialsProvider_NullConfig() throws AuthenticationException {
    var provider = authManager.getCredentialsProvider(null);
    assertNull(provider);
  }
    
  @Test
  void testAuthenticateGitHub_NullConfig() throws AuthenticationException {
    var github = authManager.authenticateGitHub(null);
    assertNotNull(github);
  }
}