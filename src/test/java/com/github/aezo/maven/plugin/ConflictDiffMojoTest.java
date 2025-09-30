package com.github.aezo.maven.plugin;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.aezo.maven.plugin.util.GitRepositoryUtil;

/**
 * Unit tests for ConflictDiffMojo.
 */
@ExtendWith(MockitoExtension.class)
class ConflictDiffMojoTest {

    @Mock
    private MavenProject project;

    @Mock
    private RepositorySystem repositorySystem;

    @Mock
    private RepositorySystemSession repoSession;

    @Mock
    private Log log;

    @Mock
    private Repository repository;

    @Mock
    private Git git;

    private ConflictDiffMojo mojo;
    private List<RemoteRepository> remoteRepos;

    @BeforeEach
    void setUp() throws Exception {
        mojo = new ConflictDiffMojo();
        remoteRepos = new ArrayList<>();

        // Set up the mojo with mocked dependencies using reflection
        setField(mojo, "project", project);
        setField(mojo, "repositorySystem", repositorySystem);
        setField(mojo, "repoSession", repoSession);
        setField(mojo, "remoteRepos", remoteRepos);
        setField(mojo, "baseBranch", "develop");
        setField(mojo, "debug", false);
        setField(mojo, "skip", false);

        // Mock the Maven project (lenient to avoid unnecessary stubbing errors)
        lenient().when(project.getBasedir()).thenReturn(new File("/test/project"));
        lenient().when(project.getArtifacts()).thenReturn(Collections.emptySet());

        // Create a test artifact for the project
        Artifact projectArtifact = new DefaultArtifact("com.example", "test-project", "1.0.0",
                "compile", "jar", "", new DefaultArtifactHandler("jar"));
        lenient().when(project.getArtifact()).thenReturn(projectArtifact);

        // Set the mojo logger directly using reflection
        mojo.setLog(log);

        // Mock Git repository operations (lenient to avoid unnecessary stubbing errors)
        lenient().when(repository.getBranch()).thenReturn("feature-branch");
        lenient().when(git.getRepository()).thenReturn(repository);
    }

    @Test
    void testSkipExecution() throws Exception {
        // Set skip to true
        setField(mojo, "skip", true);

        mojo.execute();

        verify(log).info("⏩ Skipping dependency conflict analysis");
        verifyNoInteractions(repositorySystem);
    }

    @Test
    void testSkipWhenCurrentBranchSameAsBaseBranch() throws Exception {
        try (MockedStatic<GitRepositoryUtil> gitUtil = mockStatic(GitRepositoryUtil.class)) {
            gitUtil.when(() -> GitRepositoryUtil.getRepository(any(File.class))).thenReturn(repository);
            gitUtil.when(() -> GitRepositoryUtil.getCurrentBranch(any(Git.class))).thenReturn("develop");

            mojo.execute();

            verify(log).info(contains("Current branch (develop) is the same as base branch (develop)"));
            verifyNoInteractions(repositorySystem);
        }
    }

    @Test
    void testGitRepositoryNotFoundError() throws Exception {
        try (MockedStatic<GitRepositoryUtil> gitUtil = mockStatic(GitRepositoryUtil.class)) {
            gitUtil.when(() -> GitRepositoryUtil.getRepository(any(File.class)))
                    .thenThrow(new java.io.IOException("Repository not found"));

            assertThatThrownBy(() -> mojo.execute())
                    .isInstanceOf(MojoExecutionException.class)
                    .hasMessage("❌ Failed to analyze dependency conflicts")
                    .hasCauseInstanceOf(java.io.IOException.class);
        }
    }

    // Helper methods

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
