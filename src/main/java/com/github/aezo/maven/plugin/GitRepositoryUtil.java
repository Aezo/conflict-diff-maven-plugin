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
     * @param projectBaseDir the base directory of the Maven project
     * @return the Git repository instance
     * @throws IOException if there's an error accessing the Git repository
     */
    public static Repository getRepository(File projectBaseDir) throws IOException {
        File gitDir = new File(projectBaseDir, ".git");
        if (!gitDir.exists() || !gitDir.isDirectory()) {
            throw new IOException("No Git repository found in directory: " + projectBaseDir.getAbsolutePath());
        }
        
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        return builder.setGitDir(gitDir)
                      .readEnvironment()
                      .build();
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
