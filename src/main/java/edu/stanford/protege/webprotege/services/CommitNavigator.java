package edu.stanford.protege.webprotege.services;

import edu.stanford.protege.webprotege.exceptions.RepositoryException;
import edu.stanford.protege.webprotege.model.CommitMetadata;

public interface CommitNavigator {
  CommitMetadata next() throws RepositoryException;
  CommitMetadata previous() throws RepositoryException;
  CommitMetadata nextAndCheckout() throws RepositoryException;
  CommitMetadata previousAndCheckout() throws RepositoryException;
  boolean hasNext() throws RepositoryException;
  boolean hasPrevious() throws RepositoryException;
  CommitMetadata getCurrentCommit() throws RepositoryException;
  void reset() throws RepositoryException;
}