package com.github.aezo.maven.plugin.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for GitRepositoryUtil.
 */
class GitRepositoryUtilTest {

    @TempDir
    File tempDir;

    private File testRepoDir;
    private File gitDir;
    private Git git;

    @BeforeEach
    void setUp() throws Exception {
        // Create a "test-repo" folder first
        testRepoDir = new File(tempDir, "test-repo");
        testRepoDir.mkdirs();
        
        // Create a temporary Git repository for testing inside test-repo folder
        gitDir = new File(testRepoDir, ".git");
        git = Git.init().setDirectory(testRepoDir).call();
        
        // Create an initial commit to establish HEAD reference
        // This is needed because branches cannot be created in an empty repository
        Path testFile = testRepoDir.toPath().resolve("test.txt");
        Files.write(testFile, "initial content".getBytes());
        git.add().addFilepattern("test.txt").call();
        git.commit().setMessage("Initial commit").call();
    }

    @Test
    void testGetRepository() throws IOException {
        Repository repo = GitRepositoryUtil.getRepository(testRepoDir);

        assertThat(repo).isNotNull();
        assertThat(repo.getDirectory()).isEqualTo(gitDir);
        
        repo.close();
    }

    @Test
    void testGetRepositoryWithNonExistentGitDir() {
        // Create a directory that's completely separate from our git repository
        File nonGitDir = new File(tempDir, "not-a-repo");
        nonGitDir.mkdirs();

        assertThatThrownBy(() -> GitRepositoryUtil.getRepository(nonGitDir))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("No Git repository found starting from directory");
    }

    @Test
    void testGetRepositoryWithNullDirectory() {
        assertThatThrownBy(() -> GitRepositoryUtil.getRepository(null))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("Project base directory cannot be null");
    }

    @Test
    void testGetRepositoryWithNonExistentDirectory() {
        File nonExistentDir = new File(tempDir, "does-not-exist");

        assertThatThrownBy(() -> GitRepositoryUtil.getRepository(nonExistentDir))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("Project base directory does not exist");
    }

    @Test
    void testGetRepositoryFromSubdirectory() throws Exception {
        // Create a subdirectory within the Git repository (simulating a Maven module)
        File subDir = new File(testRepoDir, "module1");
        subDir.mkdirs();
        
        // Should be able to find the Git repository from the subdirectory
        Repository repo = GitRepositoryUtil.getRepository(subDir);

        assertThat(repo).isNotNull();
        assertThat(repo.getDirectory()).isEqualTo(gitDir);
        
        repo.close();
    }

    @Test
    void testGetRepositoryFromNestedSubdirectory() throws Exception {
        // Create nested subdirectories within the Git repository
        File nestedSubDir = new File(testRepoDir, "module1/src/main/java");
        nestedSubDir.mkdirs();
        
        // Should be able to find the Git repository from the nested subdirectory
        Repository repo = GitRepositoryUtil.getRepository(nestedSubDir);

        assertThat(repo).isNotNull();
        assertThat(repo.getDirectory()).isEqualTo(gitDir);
        
        repo.close();
    }

    @Test
    void testGetCurrentBranch() throws IOException {
        String currentBranch = GitRepositoryUtil.getCurrentBranch(git);

        assertThat(currentBranch).isNotNull();
    }

    @Test
    void testCheckoutBranch() throws GitAPIException, IOException {
        // Create a new branch
        git.branchCreate().setName("test-branch").call();
        
        // Checkout the new branch
        GitRepositoryUtil.checkoutBranch(git, "test-branch");
        
        // Verify we're on the new branch
        String currentBranch = GitRepositoryUtil.getCurrentBranch(git);
        assertThat(currentBranch).isEqualTo("test-branch");
    }

    @Test
    void testCheckoutNonExistentBranch() {
        assertThatThrownBy(() -> GitRepositoryUtil.checkoutBranch(git, "non-existent-branch"))
            .isInstanceOf(GitAPIException.class);
    }
}
