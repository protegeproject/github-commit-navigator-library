package edu.stanford.protege.webprotege.services;

import edu.stanford.protege.webprotege.config.AuthenticationConfig;
import edu.stanford.protege.webprotege.exceptions.AuthenticationException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.kohsuke.github.GitHub;

public interface AuthenticationManager {
  GitHub authenticateGitHub(AuthenticationConfig config) throws AuthenticationException;
  CredentialsProvider getCredentialsProvider(AuthenticationConfig config) throws AuthenticationException;
  void validateAuthentication(AuthenticationConfig config) throws AuthenticationException;
}