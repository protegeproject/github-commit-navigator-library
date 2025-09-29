# GitHub Commit Navigator Library

A modern Java library for navigating GitHub repository commits programmatically with support for file filtering, authentication, and automatic synchronization.

## Features

- **Commit Navigation**: Navigate forward and backward through commit history with metadata
- **Auto-Sync**: Automatically pull latest changes when opening existing repositories
- **File Filtering**: Filter commits based on file changes (exact paths or glob patterns)
- **Multiple Authentication**: Support for Personal Access Token, OAuth, Username/Password, and SSH keys

## Quick Start

### Maven Dependency

```xml
<dependency>
    <groupId>edu.stanford.protege</groupId>
    <artifactId>github-commit-navigator</artifactId>
    <version>2.0.2</version>
</dependency>
```

### Basic Usage

```java
import edu.stanford.protege.commitnavigator.GitHubRepositoryBuilderFactory;
import edu.stanford.protege.commitnavigator.model.BranchCoordinates;
import edu.stanford.protege.commitnavigator.CommitNavigatorBuilder;

// Create repository coordinate from URL
var coordinate = BranchCoordinates.createFromUrl("https://github.com/example/repo");

// Create repository using factory pattern
var repository = GitHubRepositoryBuilderFactory.create(coordinate)
        .withPersonalAccessToken("your-token-here")
        .build();

// Initialize the repository
repository.initialize();

// Get the local working directory
var workingDirectory = repository.getWorkingDirectory();

// Create commit navigator using builder
var commitNavigator = CommitNavigatorBuilder.forWorkingDirectory(workingDirectory).build();

// Navigate through commits
while (commitNavigator.hasParent()) {
    var commit = commitNavigator.checkoutParent();
    System.out.println("Commit: " + commit.getCommitHash());
    System.out.println("Author: " + commit.getCommitterUsername() + " (" + commit.getCommitterEmail() + ")");
    System.out.println("Date: " + commit.getCommitDate());
    System.out.println("Message: " + commit.getCommitMessage());
    System.out.println("---");
}

// Reset navigator and navigate again
commitNavigator.reset();
while (commitNavigator.hasParent()) {
    var commit = commitNavigator.checkoutParent();
    System.out.println("Checked out commit: " + commit.getCommitHash());
}

// Clean up
repository.close();
```

## Configuration Options

```java
// Create repository coordinate for the repository and branch
var coordinate = BranchCoordinates.createFromUrl("https://github.com/example/repo", "develop");

// Configure repository with advanced options
var repository = GitHubRepositoryBuilderFactory.create(coordinate)
    .withPersonalAccessToken("your-token-here")
    .localWorkingDirectory("/path/to/local/directory")
    .shallowClone(true)
    .build();

// Initialize repository
repository.initialize();

// Get the working directory path
var workingDirectory = repository.getWorkingDirectory();

// Configure navigator with file filtering and starting position
var commitNavigator = CommitNavigatorBuilder.forWorkingDirectory(workingDirectory)
    .fileFilters("*.java", "*.md", "pom.xml") // Using convenient varargs syntax
    .startingCommit("abc123def")
    .build();
```

### Authentication

The library supports multiple authentication methods for accessing private repositories:

- `withPersonalAccessToken("your-pat-token")` - Using Personal Access Token (Recommended)
- `withOAuthToken("your-oauth-token")` - Using OAuth token
- `withUsernamePassword("username", "password")` - Using username and password
- `withSshKey("/path/to/ssh/key")` - Using SSH key

For public repositories, authentication is optional:

```java
// Create coordinate for public repository
var coordinates = BranchCoordinates.createFromUrl("https://github.com/public/repo");

// Create repository without authentication
var repository = GitHubRepositoryBuilderFactory.create(coordinates)
    .build(); // No authentication needed
```

### File Filtering

Filter commits to only include those that modified specific files using CommitNavigatorBuilder:

```java
// Configure file filters using List
var commitNavigator = CommitNavigatorBuilder.forWorkingDirectory(workingDirectory)
    .fileFilters(List.of("src/main/java/Main.java", "README.md")) // Exact file paths
    .build();

// Or using convenient varargs syntax
var commitNavigator = CommitNavigatorBuilder.forWorkingDirectory(workingDirectory)
    .fileFilters("*.java", "**/*.md", "src/**/*.xml") // Glob patterns
    .build();

// Or mixed patterns with varargs
var commitNavigator = CommitNavigatorBuilder.forWorkingDirectory(workingDirectory)
    .fileFilters("pom.xml", "*.java", "docs/**/*.md") // Mixed patterns
    .build();
```


## Navigation Methods

- `checkoutChild()` - Move to child commit (newer) and checkout working directory (returns `CommitMetadata`)
- `checkoutParent()` - Move to parent commit (older) and checkout working directory (returns `CommitMetadata`)
- `hasChild()` - Check if child commit exists
- `hasParent()` - Check if parent commit exists
- `getCurrentCommit()` - Get current commit metadata
- `getCommitCount()` - Get total number of commits in navigation sequence
- `resolveFilePath(String relativePath)` - Get absolute path for a repository-relative file path
- `reset()` - Reset navigator to initial state


## Command Line Interface

The library includes a CLI for quick repository analysis:

```bash
# Basic usage
java -jar github-commit-navigator-2.0.2.jar https://github.com/user/repo

# With authentication and filters
java -jar github-commit-navigator-2.0.2.jar \
    --token your-token \
    --file-filter "*.java,*.md" \
    --branch develop \
    --working-directory /tmp/repo \
    https://github.com/user/repo.git
```

### CLI Options

- `-t, --token`: GitHub personal access token
- `-b, --branch`: Branch to analyze (default: main)
- `-d, --working-directory`: Local working directory
- `-f, --file-filter`: Comma-separated file filters

## Building from Source

```bash
# Clone the repository
git clone https://github.com/your-org/github-commit-navigator-library.git

# Build with Maven
mvn clean compile

# Run tests
mvn test

# Create JAR
mvn package
```

### Code Quality and Formatting
```bash
# Auto-format code with Google Java Style
mvn spotless:apply

# Check code formatting
mvn spotless:check

# Run static analysis with SpotBugs
mvn spotbugs:check
```