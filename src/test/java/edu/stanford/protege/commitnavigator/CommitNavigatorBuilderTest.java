package edu.stanford.protege.commitnavigator;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.collect.Lists;
import edu.stanford.protege.commitnavigator.exceptions.RepositoryException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link CommitNavigatorBuilder}.
 *
 * <p>This test class verifies the builder pattern, configuration options, and commit navigation
 * functionality including edge cases such as filtering and empty commit scenarios.
 */
class CommitNavigatorBuilderTest {

  @TempDir private Path tempDir;

  private Path testRepoPath;
  private Git git;

  /**
   * Sets up a test Git repository with sample commits before each test.
   *
   * @throws GitAPIException if Git operations fail
   * @throws IOException if file operations fail
   */
  @BeforeEach
  void setUp() throws GitAPIException, IOException {
    testRepoPath = tempDir.resolve("test-repo");
    Files.createDirectories(testRepoPath);

    // Initialize a Git repository
    git = Git.init().setDirectory(testRepoPath.toFile()).call();

    // Create initial commit with Java file
    createFileAndCommit("Test.java", "public class Test {}", "Initial commit with Java file");

    // Create second commit with Markdown file
    createFileAndCommit("README.md", "# Test Project", "Add README");

    // Create third commit with XML file
    createFileAndCommit("pom.xml", "<project></project>", "Add Maven configuration");
  }

  /** Closes Git resources after each test. */
  @AfterEach
  void tearDown() {
    if (git != null) {
      git.close();
    }
  }

  @Test
  void testForWorkingDirectory_WithStringPath() {
    var builder = CommitNavigatorBuilder.forWorkingDirectory(testRepoPath.toString());

    assertNotNull(builder);
  }

  @Test
  void testForWorkingDirectory_WithPathObject() {
    var builder = CommitNavigatorBuilder.forWorkingDirectory(testRepoPath);

    assertNotNull(builder);
  }

  @Test
  void testForWorkingDirectory_ThrowsNullPointerException_WhenPathIsNull() {
    assertThrows(
        NullPointerException.class, () -> CommitNavigatorBuilder.forWorkingDirectory((Path) null));
  }

  @Test
  void testForWorkingDirectory_ThrowsNullPointerException_WhenStringIsNull() {
    assertThrows(
        NullPointerException.class,
        () -> CommitNavigatorBuilder.forWorkingDirectory((String) null));
  }

  @Test
  void testFileFilters_WithList() {
    var filters = Arrays.asList("*.java", "*.md");

    var builder = CommitNavigatorBuilder.forWorkingDirectory(testRepoPath).fileFilters(filters);

    assertNotNull(builder);
  }

  @Test
  void testFileFilters_WithVarargs() {
    var builder =
        CommitNavigatorBuilder.forWorkingDirectory(testRepoPath).fileFilters("*.java", "*.md");

    assertNotNull(builder);
  }

  @Test
  void testFileFilters_ThrowsNullPointerException_WhenListIsNull() {
    var builder = CommitNavigatorBuilder.forWorkingDirectory(testRepoPath);

    assertThrows(NullPointerException.class, () -> builder.fileFilters((List<String>) null));
  }

  @Test
  void testFileFilters_ThrowsNullPointerException_WhenVarargsIsNull() {
    var builder = CommitNavigatorBuilder.forWorkingDirectory(testRepoPath);

    assertThrows(NullPointerException.class, () -> builder.fileFilters((String[]) null));
  }

  @Test
  void testStartingCommit_SetsCommitHash() {
    var commitHash = "abc123def456";

    var builder =
        CommitNavigatorBuilder.forWorkingDirectory(testRepoPath).startingCommit(commitHash);

    assertNotNull(builder);
  }

  @Test
  void testStartingCommit_ThrowsNullPointerException_WhenHashIsNull() {
    var builder = CommitNavigatorBuilder.forWorkingDirectory(testRepoPath);

    assertThrows(NullPointerException.class, () -> builder.startingCommit(null));
  }

  @Test
  void testBuild_CreatesNavigatorSuccessfully() throws RepositoryException {
    var navigator = CommitNavigatorBuilder.forWorkingDirectory(testRepoPath).build();

    assertNotNull(navigator);
    assertTrue(navigator.hasParent());
  }

  @Test
  void testBuild_WithFileFilters_IncludesOnlyMatchingCommits() throws RepositoryException {
    // Filter for only Java files
    var navigator =
        CommitNavigatorBuilder.forWorkingDirectory(testRepoPath).fileFilters("*.java").build();

    assertNotNull(navigator);

    // Should only include commits that modified Java files
    var commitCount = 0;
    while (navigator.hasParent()) {
      var commit = navigator.checkoutParent();
      commitCount++;
      // Verify that at least one changed file matches the filter
      assertTrue(
          commit.getChangedFiles().stream().anyMatch(file -> file.endsWith(".java")),
          "Commit should contain Java file changes");
    }

    // Navigator collects matching commits but stops before the oldest (no parent)
    // With 1 commit collected, we get 0 navigations
    assertEquals(0, commitCount);
  }

  @Test
  void testBuild_WithMultipleFileFilters_IncludesMatchingCommits() throws RepositoryException {
    // Filter for Java and Markdown files
    var navigator =
        CommitNavigatorBuilder.forWorkingDirectory(testRepoPath)
            .fileFilters("*.java", "*.md")
            .build();

    assertNotNull(navigator);

    // Should include commits that modified Java or Markdown files
    var commitCount = 0;
    while (navigator.hasParent()) {
      var commit = navigator.checkoutParent();
      commitCount++;
    }

    // Navigator collects 2 commits but stops before the oldest (no parent)
    assertEquals(1, commitCount);
  }

  /**
   * Tests the selected lines (254-257) in CommitNavigatorBuilder.
   *
   * <p>This test verifies that a RepositoryException is thrown when no commits match the specified
   * file filters, ensuring the builder properly validates that there are commits to navigate before
   * creating the navigator.
   */
  @Test
  void testBuild_ThrowsRepositoryException_WhenNoCommitsMatchFilters() {
    // Use a filter that doesn't match any files in our test repository
    var exception =
        assertThrows(
            RepositoryException.class,
            () ->
                CommitNavigatorBuilder.forWorkingDirectory(testRepoPath)
                    .fileFilters("*.cpp", "*.h")
                    .build());

    assertTrue(exception.getMessage().contains("No relevant commits found"));
    assertTrue(exception.getMessage().contains(testRepoPath.toString()));
  }

  @Test
  void testBuild_ThrowsException_WhenDirectoryIsNotGitRepository() throws IOException {
    // Create a directory that is not a Git repository
    var nonGitDir = tempDir.resolve("non-git-dir");
    Files.createDirectories(nonGitDir);

    // Should throw either RepositoryException or NullPointerException
    // depending on the state of the repository
    assertThrows(
        Exception.class, () -> CommitNavigatorBuilder.forWorkingDirectory(nonGitDir).build());
  }

  @Test
  void testBuild_WithStartingCommit_NavigatesFromSpecificCommit()
      throws RepositoryException, GitAPIException {
    // Get the commit hash of the second commit (middle commit)
    var commits = git.log().call();
    var commitIterator = commits.iterator();
    commitIterator.next(); // Skip first (most recent - pom.xml)
    var secondCommit = commitIterator.next(); // Second commit (README.md)
    var secondCommitHash = secondCommit.getName();

    // Build navigator starting from second commit
    var navigator =
        CommitNavigatorBuilder.forWorkingDirectory(testRepoPath)
            .startingCommit(secondCommitHash)
            .build();

    assertNotNull(navigator);
    assertTrue(navigator.hasParent());

    // Navigate and verify we can still move through commits
    var commit = navigator.checkoutParent();
    assertNotNull(commit);
    assertNotNull(commit.getCommitHash());
  }

  @Test
  void testBuild_WithNonExistentStartingCommit_FallsBackToHead() throws RepositoryException {
    // Use a non-existent commit hash
    var nonExistentHash = "0000000000000000000000000000000000000000";

    var navigator =
        CommitNavigatorBuilder.forWorkingDirectory(testRepoPath)
            .startingCommit(nonExistentHash)
            .build();

    assertNotNull(navigator);
    // Should start from HEAD since the commit doesn't exist
    assertTrue(navigator.hasParent());
  }

  @Test
  void testBuild_CollectsCommitsInChronologicalOrder() throws RepositoryException {
    var navigator = CommitNavigatorBuilder.forWorkingDirectory(testRepoPath).build();

    var commitMessages = Lists.<String>newArrayList();
    while (navigator.hasParent()) {
      var commit = navigator.checkoutParent();
      commitMessages.add(commit.getCommitMessage().trim());
    }

    // Verify we got commits (navigator returns parent commits)
    // Navigator starts at HEAD and returns parents, stopping before the oldest
    assertEquals(2, commitMessages.size());
    assertEquals("Add README", commitMessages.get(0));
    assertEquals("Initial commit with Java file", commitMessages.get(1));
  }

  @Test
  void testBuild_WithEmptyFileFilters_IncludesAllCommits() throws RepositoryException {
    var navigator =
        CommitNavigatorBuilder.forWorkingDirectory(testRepoPath)
            .fileFilters(java.util.Collections.emptyList())
            .build();

    assertNotNull(navigator);

    var commitCount = 0;
    while (navigator.hasParent()) {
      navigator.checkoutParent();
      commitCount++;
    }

    // Navigator collects 3 commits but stops before the oldest (no parent)
    assertEquals(2, commitCount);
  }

  /**
   * Helper method to create a file and commit it to the test repository.
   *
   * @param fileName the name of the file to create
   * @param content the content to write to the file
   * @param commitMessage the commit message
   * @throws IOException if file operations fail
   * @throws GitAPIException if Git operations fail
   */
  private void createFileAndCommit(String fileName, String content, String commitMessage)
      throws IOException, GitAPIException {
    var filePath = testRepoPath.resolve(fileName);
    Files.writeString(filePath, content);
    git.add().addFilepattern(fileName).call();
    git.commit().setMessage(commitMessage).call();
  }
}
