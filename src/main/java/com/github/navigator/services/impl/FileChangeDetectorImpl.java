package com.github.navigator.services.impl;

import com.github.navigator.exceptions.RepositoryException;
import com.github.navigator.services.FileChangeDetector;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class FileChangeDetectorImpl implements FileChangeDetector {
    private static final Logger logger = LoggerFactory.getLogger(FileChangeDetectorImpl.class);

    @Override
    public boolean hasFileChanges(Repository repository, RevCommit commit, List<String> fileFilters) throws RepositoryException {
        if (fileFilters == null || fileFilters.isEmpty()) {
            return true;
        }
        
        try {
            List<String> changedFiles = getChangedFiles(repository, commit);
            return changedFiles.stream().anyMatch(file -> matchesAnyFilter(file, fileFilters));
        } catch (Exception e) {
            throw new RepositoryException("Failed to check file changes in commit " + commit.getName(), e);
        }
    }

    @Override
    public List<String> getChangedFiles(Repository repository, RevCommit commit) throws RepositoryException {
        logger.debug("Getting changed files for commit: {}", commit.getName());
        
        try (RevWalk revWalk = new RevWalk(repository);
             DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
            
            diffFormatter.setRepository(repository);
            
            RevCommit[] parents = commit.getParents();
            List<DiffEntry> diffs = new ArrayList<>();
            
            if (parents.length == 0) {
                diffs = diffFormatter.scan(getEmptyTreeIterator(repository), 
                                         getTreeIterator(repository, commit));
            } else {
                for (RevCommit parent : parents) {
                    revWalk.parseCommit(parent);
                    List<DiffEntry> parentDiffs = diffFormatter.scan(
                        getTreeIterator(repository, parent),
                        getTreeIterator(repository, commit)
                    );
                    diffs.addAll(parentDiffs);
                }
            }
            
            return diffs.stream()
                    .map(diff -> getFilePath(diff))
                    .filter(path -> path != null && !path.isEmpty())
                    .collect(Collectors.toList());
            
        } catch (IOException e) {
            throw new RepositoryException("Failed to get changed files for commit " + commit.getName(), e);
        }
    }

    boolean matchesAnyFilter(String filePath, List<String> fileFilters) {
        if (fileFilters == null || fileFilters.isEmpty()) {
            return true;
        }
        return fileFilters.stream().anyMatch(filter -> matchesFilter(filePath, filter));
    }

    boolean matchesFilter(String filePath, String filter) {
        if (filter.contains("*") || filter.contains("?")) {
            try {
                PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + filter);
                return matcher.matches(FileSystems.getDefault().getPath(filePath));
            } catch (Exception e) {
                logger.warn("Invalid glob pattern: {}", filter, e);
                return false;
            }
        } else {
            return filePath.equals(filter) || filePath.endsWith("/" + filter);
        }
    }

    private String getFilePath(DiffEntry diff) {
        switch (diff.getChangeType()) {
            case ADD:
            case MODIFY:
            case COPY:
                return diff.getNewPath();
            case DELETE:
                return diff.getOldPath();
            case RENAME:
                return diff.getNewPath();
            default:
                return diff.getNewPath();
        }
    }

    private AbstractTreeIterator getTreeIterator(Repository repository, RevCommit commit) throws IOException {
        RevTree tree = commit.getTree();
        CanonicalTreeParser treeParser = new CanonicalTreeParser();
        treeParser.reset(repository.newObjectReader(), tree);
        return treeParser;
    }

    private AbstractTreeIterator getEmptyTreeIterator(Repository repository) throws IOException {
        return new EmptyTreeIterator();
    }
}