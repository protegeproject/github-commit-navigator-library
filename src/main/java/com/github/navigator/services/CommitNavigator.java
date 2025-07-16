package com.github.navigator.services;

import com.github.navigator.exceptions.RepositoryException;

public interface CommitNavigator {
    String next() throws RepositoryException;
    String previous() throws RepositoryException;
    String nextAndCheckout() throws RepositoryException;
    String previousAndCheckout() throws RepositoryException;
    boolean hasNext() throws RepositoryException;
    boolean hasPrevious() throws RepositoryException;
    String getCurrentCommit() throws RepositoryException;
    void reset() throws RepositoryException;
}