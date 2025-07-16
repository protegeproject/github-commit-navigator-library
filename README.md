# GitHub Commit Navigator Library

A Java library for navigating GitHub repository commits programmatically with support for file filtering and authentication.

## Features

- **Commit Navigation**: Navigate forward and backward through commit history
- **File Filtering**: Filter commits based on file changes (exact paths or glob patterns)
- **Authentication**: Support for Personal Access Token, OAuth, Username/Password, and SSH keys
- **Flexible Configuration**: Builder pattern for easy configuration
- **Checkout Support**: Optional checkout during navigation
- **Dependency Injection**: Clean architecture with DI support

## Quick Start

### Maven Dependency

```xml
<dependency>
    <groupId>com.github.navigator</groupId>
    <artifactId>github-commit-navigator</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Basic Usage

```java
import com.github.navigator.GitHubRepoNavigator;
import com.github.navigator.GitHubRepoNavigatorBuilder;
import com.github.navigator.services.CommitNavigator;

// Create navigator with Personal Access Token
GitHubRepoNavigator navigator = GitHubRepoNavigatorBuilder
    .forRepository("https://github.com/example/repo.git")
    .withPersonalAccessToken("your-token-here")
    .build();

// Initialize the navigator
navigator.initialize();

// Get commit navigator
CommitNavigator commitNavigator = navigator.getCommitNavigator();

// Navigate through commits
while (commitNavigator.hasNext()) {
    String commitHash = commitNavigator.next();
    System.out.println("Next commit: " + commitHash);
}

// Navigate with checkout
while (commitNavigator.hasPrevious()) {
    String commitHash = commitNavigator.previousAndCheckout();
    System.out.println("Checked out commit: " + commitHash);
}

// Clean up
navigator.close();
```

### Advanced Configuration

```java
GitHubRepoNavigator navigator = GitHubRepoNavigatorBuilder
    .forRepository("https://github.com/example/repo.git")
    .withPersonalAccessToken("your-token-here")
    .localCloneDirectory("/path/to/local/repo")
    .fileFilters("*.java", "*.md", "pom.xml")
    .branch("develop")
    .startingCommit("abc123def")
    .shallowClone(true)
    .build();
```

## Authentication

### Personal Access Token (Recommended)

```java
.withPersonalAccessToken("your-pat-token")
```

### OAuth Token

```java
.withOAuthToken("your-oauth-token")
```

### Username/Password

```java
.withUsernamePassword("username", "password")
```

### SSH Key

```java
.withSshKey("/path/to/ssh/key")
```

## Navigation Methods

### Basic Navigation

- `next()` - Move to next commit (no checkout)
- `previous()` - Move to previous commit (no checkout)
- `hasNext()` - Check if next commit exists
- `hasPrevious()` - Check if previous commit exists
- `getCurrentCommit()` - Get current commit hash

### Navigation with Checkout

- `nextAndCheckout()` - Move to next commit and checkout
- `previousAndCheckout()` - Move to previous commit and checkout

## File Filtering

Filter commits based on file changes:

```java
// Exact file paths
.fileFilters("src/main/java/Main.java", "README.md")

// Glob patterns
.fileFilters("*.java", "**/*.md", "src/**/*.xml")

// Mixed patterns
.fileFilters("pom.xml", "*.java", "docs/**/*.md")
```

## Error Handling

The library uses checked exceptions for error handling:

```java
try {
    navigator.initialize();
    CommitNavigator commitNavigator = navigator.getCommitNavigator();
    
    String commit = commitNavigator.next();
    // Handle commit
    
} catch (AuthenticationException e) {
    // Handle authentication errors
} catch (RepositoryException e) {
    // Handle repository errors  
} catch (GitHubNavigatorException e) {
    // Handle general navigation errors
}
```

## Dependencies

- **JGit** (Eclipse JGit) - Git operations
- **GitHub API for Java** - GitHub authentication
- **SLF4J** - Logging
- **JUnit 5** - Testing (test scope)
- **Mockito** - Mocking (test scope)

## Architecture

The library follows clean architecture principles:

- **Facade Pattern**: `GitHubRepoNavigator` provides a simplified interface
- **Strategy Pattern**: `AuthenticationManager` handles different auth types
- **Dependency Injection**: Uses `@Inject` annotations for DI
- **Builder Pattern**: Easy configuration with fluent API
- **Interface Segregation**: Separate interfaces for different concerns

## Thread Safety

The library is designed to be thread-safe for read operations. However, navigation state is not thread-safe - use separate navigator instances for concurrent access.

## License

This library is released under the MIT License.