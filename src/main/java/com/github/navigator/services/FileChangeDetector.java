package com.github.navigator.services;

import com.github.navigator.exceptions.RepositoryException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import java.util.List;

public interface FileChangeDetector {
    boolean hasFileChanges(Repository repository, RevCommit commit, List<String> fileFilters) throws RepositoryException;
    List<String> getChangedFiles(Repository repository, RevCommit commit) throws RepositoryException;
}