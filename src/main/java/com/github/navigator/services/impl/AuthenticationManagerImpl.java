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
            GitHubBuilder builder = new GitHubBuilder();
            
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
                if (config.getToken() == null || config.getToken().trim().isEmpty()) {
                    throw new AuthenticationException("Token cannot be null or empty");
                }
                break;
                
            case USERNAME_PASSWORD:
                if (config.getUsername() == null || config.getUsername().trim().isEmpty()) {
                    throw new AuthenticationException("Username cannot be null or empty");
                }
                if (config.getPassword() == null || config.getPassword().trim().isEmpty()) {
                    throw new AuthenticationException("Password cannot be null or empty");
                }
                break;
                
            case SSH_KEY:
                if (config.getSshKeyPath() == null || config.getSshKeyPath().trim().isEmpty()) {
                    throw new AuthenticationException("SSH key path cannot be null or empty");
                }
                break;
                
            default:
                throw new AuthenticationException("Unsupported authentication type: " + config.getType());
        }
        
        try {
            GitHub github = authenticateGitHub(config);
            github.checkApiUrlValidity();
            github.getMyself();
        } catch (IOException e) {
            throw new AuthenticationException("Authentication validation failed", e);
        }
    }
}