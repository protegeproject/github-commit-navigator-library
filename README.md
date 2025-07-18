# GitHub Commit Navigator Library

A modern Java library for navigating GitHub repository commits programmatically with support for file filtering, authentication, and automatic synchronization.

## Features

- **Commit Navigation**: Navigate forward and backward through commit history with metadata
- **Auto-Sync**: Automatically pull latest changes when opening existing repositories
- **File Filtering**: Filter commits based on file changes (exact paths or glob patterns)
- **Multiple Authentication**: Support for Personal Access Token, OAuth, Username/Password, and SSH keys
- **Flexible Configuration**: Builder pattern for easy configuration
- **Checkout Support**: Optional checkout during navigation

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
import edu.stanford.protege.commitnavigator.GitHubRepositoryBuilder;
import edu.stanford.protege.commitnavigator.services.CommitNavigator;
import edu.stanford.protege.commitnavigator.model.CommitMetadata;

// Create navigator with Personal Access Token
var repository = GitHubRepositoryBuilder
    .forRepository("https://github.com/example/repo.git")
    .withPersonalAccessToken("your-token-here")
    .build();

// Initialize the navigator
repository.initialize();

// Get commit navigator
var commitNavigator = navigator.getCommitNavigator();

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
navigator.close();
```

### Advanced Configuration

```java
var navigator = GitHubRepoNavigatorBuilder
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

The library supports multiple authentication methods for accessing private repositories:

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

### Anonymous Access

For public repositories, authentication is optional:

```java
var navigator = GitHubRepositoryBuilder
    .forRepository("https://github.com/public/repo.git")
    .build(); // No authentication needed
```

## Navigation Methods

### Basic Navigation

- `next()` - Move to next commit (returns `CommitMetadata`)
- `previous()` - Move to previous commit (returns `CommitMetadata`)
- `hasNext()` - Check if next commit exists
- `hasPrevious()` - Check if previous commit exists
- `getCurrentCommit()` - Get current commit metadata
- `reset()` - Reset navigator to initial state

### Navigation with Checkout

- `nextAndCheckout()` - Move to next commit and checkout working directory
- `previousAndCheckout()` - Move to previous commit and checkout working directory

### Commit Metadata

The `CommitMetadata` record provides comprehensive commit information:

```java
var commit = commitNavigator.next();
var hash = commit.getCommitHash();           // Full commit SHA
var author = commit.getCommitterUsername();  // Committer username  
var date = commit.getCommitDate();           // LocalDateTime of commit
var message = commit.getCommitMessage();     // Full commit message
```

## File Filtering

Filter commits to only include those that modified specific files:

```java
// Exact file paths
.fileFilters("src/main/java/Main.java", "README.md")

// Glob patterns
.fileFilters("*.java", "**/*.md", "src/**/*.xml")

// Mixed patterns
.fileFilters("pom.xml", "*.java", "docs/**/*.md")
```

## Automatic Repository Synchronization

When opening an existing local repository, the library automatically:

1. Fetches latest changes from remote
2. Compares local vs remote commits
3. Pulls new commits if available
4. Logs all sync operations

```
DEBUG - Checking for remote changes...
INFO  - New changes detected on remote branch 'main', pulling changes...
INFO  - Successfully pulled 3 new commits from remote
```


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

## Requirements

- **Java 17+** (uses modern Java features)
- **Maven 3.6+** for building
- **Git** installed on system (for JGit operations)

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