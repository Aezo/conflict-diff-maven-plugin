package com.github.aezo.maven.plugin;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

/**
 * Utility class for Git repository operations.
 * 
 * This class provides common Git repository access functionality
 * that can be shared across different components.
 */
public class GitRepositoryUtil {

    /**
     * Gets a Git repository instance for the given project base directory.
     * 
     * This method handles various scenarios:
     * - Project root with Git repository
     * - Multi-module Maven projects where Git repo is in parent directory
     * - Submodules and Git worktrees
     * - Temporary directories (for testing)
     * 
     * @param projectBaseDir the base directory of the Maven project
     * @return the Git repository instance
     * @throws IOException if there's an error accessing the Git repository or no Git repository is found
     */
    public static Repository getRepository(File projectBaseDir) throws IOException {
        if (projectBaseDir == null) {
            throw new IOException("Project base directory cannot be null");
        }
        
        if (!projectBaseDir.exists()) {
            throw new IOException("Project base directory does not exist: " + projectBaseDir.getAbsolutePath());
        }
        
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        
        try {
            // Start from the project directory and traverse upwards to find Git repository
            // Use findGitDir with starting directory for better multi-module support
            Repository repository = builder
                    .readEnvironment()
                    .findGitDir(projectBaseDir)  // Start search from project directory
                    .build();
            
            return repository;
        } catch (IllegalArgumentException e) {
            // Handle case where no Git repository is found
            throw new IOException("No Git repository found starting from directory: " + 
                projectBaseDir.getAbsolutePath() + ". Ensure you're running from within a Git repository.", e);
        } catch (IOException e) {
            // Provide more descriptive error messages
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                throw new IOException("No Git repository found starting from directory: " + 
                    projectBaseDir.getAbsolutePath() + ". Ensure you're running from within a Git repository.", e);
            }
            throw new IOException("Failed to access Git repository from directory: " + 
                projectBaseDir.getAbsolutePath() + ". " + e.getMessage(), e);
        }
    }

    /**
     * Gets the current branch name from the Git repository.
     * 
     * @param git the Git instance
     * @return the current branch name
     * @throws IOException if there's an error reading the branch information
     */
    public static String getCurrentBranch(Git git) throws IOException {
        return git.getRepository().getBranch();
    }

    /**
     * Checks out the specified branch.
     * 
     * @param git the Git instance
     * @param branchName the name of the branch to checkout
     * @throws GitAPIException if there's an error during the checkout operation
     */
    public static void checkoutBranch(Git git, String branchName) throws GitAPIException {
        git.checkout().setName(branchName).call();
    }
}
