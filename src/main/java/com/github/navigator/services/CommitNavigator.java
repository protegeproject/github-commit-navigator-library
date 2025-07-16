package com.github.navigator.services;

import com.github.navigator.exceptions.RepositoryException;
import com.github.navigator.model.CommitMetadata;

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