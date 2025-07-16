package com.github.navigator.services;

import com.github.navigator.config.AuthenticationConfig;
import com.github.navigator.exceptions.AuthenticationException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.kohsuke.github.GitHub;

public interface AuthenticationManager {
    GitHub authenticateGitHub(AuthenticationConfig config) throws AuthenticationException;
    CredentialsProvider getCredentialsProvider(AuthenticationConfig config) throws AuthenticationException;
    void validateAuthentication(AuthenticationConfig config) throws AuthenticationException;
}