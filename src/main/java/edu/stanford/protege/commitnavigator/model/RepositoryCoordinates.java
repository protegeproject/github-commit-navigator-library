package edu.stanford.protege.commitnavigator.model;

public record RepositoryCoordinates(String ownerName, String repositoryName, String branchName) {

  public static final String DEFAULT_BRANCH = "main";

  public static RepositoryCoordinates create(
      String ownerName, String repositoryName, String branchName) {
    return new RepositoryCoordinates(ownerName, repositoryName, branchName);
  }

  public static RepositoryCoordinates create(String ownerName, String repositoryName) {
    return create(ownerName, repositoryName, DEFAULT_BRANCH);
  }

  public static RepositoryCoordinates createFromUrl(String repositoryUrl, String branchName) {
    // Remove protocol if present
    var url = repositoryUrl.replaceFirst("^https?://github\\.com/", "");
    // Remove .git suffix if present
    if (url.endsWith(".git")) {
      url = url.substring(0, url.length() - 4);
    }

    var parts = url.split("/");
    if (parts.length < 2) {
      throw new IllegalArgumentException("Invalid GitHub repository URL: " + repositoryUrl);
    }

    var ownerName = parts[0];
    var repositoryName = parts[1];

    return create(ownerName, repositoryName, branchName);
  }

  public static RepositoryCoordinates createFromUrl(String repositoryUrl) {
    return createFromUrl(repositoryUrl, DEFAULT_BRANCH);
  }

  /** Builds the GitHub repository URL from the repository coordinate. */
  public String gitHubUrl() {
    return String.format("https://github.com/%s/%s.git", ownerName, repositoryName);
  }
}
