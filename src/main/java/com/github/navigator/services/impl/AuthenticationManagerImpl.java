package com.github.navigator.services.impl;

import com.github.navigator.config.AuthenticationConfig;
import com.github.navigator.exceptions.AuthenticationException;
import com.github.navigator.services.AuthenticationManager;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

@Singleton
public class AuthenticationManagerImpl implements AuthenticationManager {
  private static final Logger logger = LoggerFactory.getLogger(AuthenticationManagerImpl.class);

  @Override
  public GitHub authenticateGitHub(AuthenticationConfig config) throws AuthenticationException {
    if (config == null) {
      logger.debug("No authentication config provided, connecting to GitHub anonymously");
      try {
        return new GitHubBuilder().build();
      } catch (IOException e) {
        throw new AuthenticationException("Failed to connect to GitHub anonymously", e);
      }
    }
    
    logger.debug("Authenticating with GitHub using {}", config.getType());
    
    try {
      var builder = new GitHubBuilder();
      
      switch (config.getType()) {
        case PERSONAL_ACCESS_TOKEN:
        case OAUTH:
          return builder.withOAuthToken(config.getToken()).build();
          
        case USERNAME_PASSWORD:
          return builder.withPassword(config.getUsername(), config.getPassword()).build();
          
        case SSH_KEY:
          throw new AuthenticationException("SSH key authentication not supported for GitHub API");
          
        default:
          throw new AuthenticationException("Unsupported authentication type: " + config.getType());
      }
    } catch (IOException e) {
      throw new AuthenticationException("Failed to authenticate with GitHub", e);
    }
  }

  @Override
  public CredentialsProvider getCredentialsProvider(AuthenticationConfig config) throws AuthenticationException {
    if (config == null) {
      logger.debug("No authentication config provided, returning null credentials provider for anonymous access");
      return null;
    }
    
    logger.debug("Creating credentials provider for {}", config.getType());
    
    switch (config.getType()) {
      case PERSONAL_ACCESS_TOKEN:
      case OAUTH:
        return new UsernamePasswordCredentialsProvider(config.getToken(), "");
        
      case USERNAME_PASSWORD:
        return new UsernamePasswordCredentialsProvider(config.getUsername(), config.getPassword());
        
      case SSH_KEY:
        throw new AuthenticationException("SSH key authentication not yet implemented");
        
      default:
        throw new AuthenticationException("Unsupported authentication type: " + config.getType());
    }
  }

  @Override
  public void validateAuthentication(AuthenticationConfig config) throws AuthenticationException {
    logger.debug("Validating authentication configuration");
    
    if (config == null) {
      logger.debug("No authentication config provided, validation skipped for anonymous access");
      return;
    }
    
    switch (config.getType()) {
      case PERSONAL_ACCESS_TOKEN:
      case OAUTH:
        validateToken(config.getToken());
        break;
        
      case USERNAME_PASSWORD:
        validateUsernamePassword(config.getUsername(), config.getPassword());
        break;
        
      case SSH_KEY:
        validateSshKeyPath(config.getSshKeyPath());
        break;
        
      default:
        throw new AuthenticationException("Unsupported authentication type: " + config.getType());
    }
    
    try {
      var github = authenticateGitHub(config);
      github.checkApiUrlValidity();
      github.getMyself();
    } catch (IOException e) {
      throw new AuthenticationException("Authentication validation failed", e);
    }
  }

  private void validateToken(String token) throws AuthenticationException {
    if (Optional.ofNullable(token).map(String::trim).filter(t -> !t.isEmpty()).isEmpty()) {
      throw new AuthenticationException("Token cannot be null or empty");
    }
  }

  private void validateUsernamePassword(String username, String password) throws AuthenticationException {
    if (Optional.ofNullable(username).map(String::trim).filter(u -> !u.isEmpty()).isEmpty()) {
      throw new AuthenticationException("Username cannot be null or empty");
    }
    if (Optional.ofNullable(password).map(String::trim).filter(p -> !p.isEmpty()).isEmpty()) {
      throw new AuthenticationException("Password cannot be null or empty");
    }
  }

  private void validateSshKeyPath(String sshKeyPath) throws AuthenticationException {
    if (Optional.ofNullable(sshKeyPath).map(String::trim).filter(path -> !path.isEmpty()).isEmpty()) {
      throw new AuthenticationException("SSH key path cannot be null or empty");
    }
  }
}