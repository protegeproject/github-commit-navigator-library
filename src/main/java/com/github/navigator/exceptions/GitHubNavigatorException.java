package com.github.navigator.exceptions;

public class GitHubNavigatorException extends Exception {
    public GitHubNavigatorException(String message) {
        super(message);
    }

    public GitHubNavigatorException(String message, Throwable cause) {
        super(message, cause);
    }
}