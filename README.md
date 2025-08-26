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
    <groupId>edu.stanford.protege.commitnavigator</groupId>
    <artifactId>github-commit-navigator</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Basic Usage

```java
import edu.stanford.protege.commitnavigator.GitHubRepository;
import edu.stanford.protege.commitnavigator.GitHubRepositoryBuilderFactory;
import edu.stanford.protege.commitnavigator.model.RepositoryCoordinate;
import edu.stanford.protege.commitnavigator.utils.CommitNavigator;
import edu.stanford.protege.commitnavigator.model.CommitMetadata;

// Create repository coordinate from URL
var coordinate = RepositoryCoordinate.createFromUrl("https://github.com/example/repo.git");

// Create repository using factory pattern
var repository = GitHubRepositoryBuilderFactory.create(coordinate)
    .withPersonalAccessToken("your-token-here")
    .build();

// Initialize the navigator
repository.initialize();

// Get commit navigator
var commitNavigator = repository.getCommitNavigator();

// Navigate through commits
while (commitNavigator.hasNext()) {
    var commit = commitNavigator.next();
    System.out.println("Commit: " + commit.getCommitHash());
    System.out.println("Author: " + commit.getCommitterUsername());
    System.out.println("Date: " + commit.getCommitDate());
    System.out.println("Message: " + commit.getCommitMessage());
    System.out.println("---");
}

// Navigate with checkout
while (commitNavigator.hasPrevious()) {
    var commit = commitNavigator.previousAndCheckout();
    System.out.println("Checked out commit: " + commit.getCommitHash());
}

// Clean up
repository.close();
```

## Configuration Options

```java
// Create repository coordinate for the repository and branch
var coordinate = RepositoryCoordinate.createFromUrl("https://github.com/example/repo.git", "develop");

// Configure repository with advanced options
var repository = GitHubRepositoryBuilderFactory.create(coordinate)
    .withPersonalAccessToken("your-token-here")
    .localCloneDirectory("/path/to/local/repo")
    .fileFilters("*.java", "*.md", "pom.xml")
    .startingCommit("abc123def")
    .shallowClone(true)
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
var coordinate = RepositoryCoordinate.createFromUrl("https://github.com/public/repo.git");

// Create repository without authentication
var repository = GitHubRepositoryBuilderFactory.create(coordinate)
    .build(); // No authentication needed
```

### File Filtering

Filter commits to only include those that modified specific files:

- `fileFilters("src/main/java/Main.java", "README.md")` - Exact file paths
- `fileFilters("*.java", "**/*.md", "src/**/*.xml")` - Glob patterns
- `fileFilters("pom.xml", "*.java", "docs/**/*.md")` - Mixed patterns


## Navigation Methods

- `next()` - Move to next commit (returns `CommitMetadata`)
- `previous()` - Move to previous commit (returns `CommitMetadata`)
- `hasNext()` - Check if next commit exists
- `hasPrevious()` - Check if previous commit exists
- `getCurrentCommit()` - Get current commit metadata
- `reset()` - Reset navigator to initial state
- `nextAndCheckout()` - Move to next commit and checkout working directory
- `previousAndCheckout()` - Move to previous commit and checkout working directory


## Command Line Interface

The library includes a CLI for quick repository analysis:

```bash
# Basic usage
java -jar github-commit-navigator-1.0.0.jar https://github.com/user/repo.git

# With authentication and filters
java -jar github-commit-navigator-1.0.0.jar \
    --token your-token \
    --file-filter "*.java,*.md" \
    --branch develop \
    --clone-directory /tmp/repo \
    https://github.com/user/repo.git
```

### CLI Options

- `-t, --token`: GitHub personal access token
- `-b, --branch`: Branch to analyze (default: main)
- `-d, --clone-directory`: Local clone directory
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