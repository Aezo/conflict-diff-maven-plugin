package com.github.aezo.maven.plugin;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.aezo.maven.plugin.model.DependencyConflict;
import com.github.aezo.maven.plugin.model.VersionConflict;
import com.github.aezo.maven.plugin.strategy.ConflictDetectionStrategy;
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
    void testConflictDetectionWithNewConflicts() throws Exception {
        // Setup: Create resolved artifacts that match what we'll find in dependency
        // graph
        Set<Artifact> resolvedArtifacts = new HashSet<>();
        resolvedArtifacts.add(createArtifact("com.example", "lib1", "1.1.0")); // Winning version
        resolvedArtifacts.add(createArtifact("com.example", "lib2", "2.0.0"));
        when(project.getArtifacts()).thenReturn(resolvedArtifacts);

        // Create base branch dependency graph (no conflicts)
        DependencyNode baseBranchRoot = createDependencyGraph(
                createMockArtifact("com.example", "lib1", "1.1.0"),
                createMockArtifact("com.example", "lib2", "2.0.0"));

        // Create current branch dependency graph (with conflicts)
        DependencyNode currentBranchRoot = createDependencyGraph(
                createMockArtifact("com.example", "lib1", "1.0.0"), // Conflict: older version in graph
                createMockArtifact("com.example", "lib1", "1.1.0"), // Conflict: winning version
                createMockArtifact("com.example", "lib2", "2.0.0"));

        CollectResult baseResult = mock(CollectResult.class);
        when(baseResult.getRoot()).thenReturn(baseBranchRoot);

        CollectResult currentResult = mock(CollectResult.class);
        when(currentResult.getRoot()).thenReturn(currentBranchRoot);

        // Mock repository system to return different results for each branch checkout
        when(repositorySystem.collectDependencies(eq(repoSession), any(CollectRequest.class)))
                .thenReturn(baseResult) // First call for base branch
                .thenReturn(currentResult); // Second call for current branch

        try (MockedStatic<GitRepositoryUtil> gitUtil = mockStatic(GitRepositoryUtil.class)) {
            gitUtil.when(() -> GitRepositoryUtil.getRepository(any(File.class))).thenReturn(repository);
            gitUtil.when(() -> GitRepositoryUtil.getCurrentBranch(any(Git.class))).thenReturn("feature-branch");
            gitUtil.when(() -> GitRepositoryUtil.checkoutBranch(any(Git.class), anyString())).then(invocation -> null);

            mojo.execute();

            // Verify the analysis was performed
            verify(log).info("⏳ Analyzing dependency conflicts between 'develop' and 'feature-branch'");

            // Verify new conflicts were detected and reported
            verify(log).info("⚠️  Transitive dependency conflict differences found between branches:");
            verify(log).info("❌ NEW CONFLICTS (present in current branch but not in base branch):");

            // Verify summary was printed
            verify(log).info(contains("📋 SUMMARY:"));
            verify(log).info(contains("1 new"));
        }
    }

    @Test
    void testConflictDetectionWithResolvedConflicts() throws Exception {
        // Setup: Create resolved artifacts
        Set<Artifact> resolvedArtifacts = new HashSet<>();
        resolvedArtifacts.add(createArtifact("com.example", "lib1", "1.1.0"));
        when(project.getArtifacts()).thenReturn(resolvedArtifacts);

        // Create base branch dependency graph (with conflicts)
        DependencyNode baseBranchRoot = createDependencyGraph(
                createMockArtifact("com.example", "lib1", "1.0.0"), // Conflict
                createMockArtifact("com.example", "lib1", "1.1.0") // Winning version
        );

        // Create current branch dependency graph (no conflicts)
        DependencyNode currentBranchRoot = createDependencyGraph(
                createMockArtifact("com.example", "lib1", "1.1.0") // Only winning version
        );

        CollectResult baseResult = mock(CollectResult.class);
        when(baseResult.getRoot()).thenReturn(baseBranchRoot);

        CollectResult currentResult = mock(CollectResult.class);
        when(currentResult.getRoot()).thenReturn(currentBranchRoot);

        when(repositorySystem.collectDependencies(eq(repoSession), any(CollectRequest.class)))
                .thenReturn(baseResult)
                .thenReturn(currentResult);

        try (MockedStatic<GitRepositoryUtil> gitUtil = mockStatic(GitRepositoryUtil.class)) {
            gitUtil.when(() -> GitRepositoryUtil.getRepository(any(File.class))).thenReturn(repository);
            gitUtil.when(() -> GitRepositoryUtil.getCurrentBranch(any(Git.class))).thenReturn("feature-branch");
            gitUtil.when(() -> GitRepositoryUtil.checkoutBranch(any(Git.class), anyString())).then(invocation -> null);

            mojo.execute();

            // Verify resolved conflicts were detected and reported
            verify(log).info("⚠️  Transitive dependency conflict differences found between branches:");
            verify(log).info("✅ RESOLVED CONFLICTS (present in base branch but not in current branch):");
            verify(log).info(contains("📋 SUMMARY:"));
            verify(log).info(contains("1 resolved"));
        }
    }

    @Test
    void testConflictDetectionWithChangedConflicts() throws Exception {
        // Setup: Create resolved artifacts
        Set<Artifact> resolvedArtifacts = new HashSet<>();
        resolvedArtifacts.add(createArtifact("com.example", "lib1", "1.2.0")); // Same winning version
        when(project.getArtifacts()).thenReturn(resolvedArtifacts);

        // Create base branch dependency graph (conflict with 1 occurrence)
        DependencyNode baseBranchRoot = createDependencyGraph(
                createMockArtifact("com.example", "lib1", "1.0.0"), // Conflict - 1 occurrence
                createMockArtifact("com.example", "lib1", "1.2.0") // Winning version
        );

        // Create current branch dependency graph (same conflict but 2 occurrences)
        DependencyNode currentBranchRoot = createDependencyGraph(
                createMockArtifact("com.example", "lib1", "1.0.0"), // Conflict - 1st occurrence
                createMockArtifact("com.example", "lib1", "1.0.0"), // Conflict - 2nd occurrence
                createMockArtifact("com.example", "lib1", "1.2.0") // Winning version
        );

        CollectResult baseResult = mock(CollectResult.class);
        when(baseResult.getRoot()).thenReturn(baseBranchRoot);

        CollectResult currentResult = mock(CollectResult.class);
        when(currentResult.getRoot()).thenReturn(currentBranchRoot);

        when(repositorySystem.collectDependencies(eq(repoSession), any(CollectRequest.class)))
                .thenReturn(baseResult)
                .thenReturn(currentResult);

        try (MockedStatic<GitRepositoryUtil> gitUtil = mockStatic(GitRepositoryUtil.class)) {
            gitUtil.when(() -> GitRepositoryUtil.getRepository(any(File.class))).thenReturn(repository);
            gitUtil.when(() -> GitRepositoryUtil.getCurrentBranch(any(Git.class))).thenReturn("feature-branch");
            gitUtil.when(() -> GitRepositoryUtil.checkoutBranch(any(Git.class), anyString())).then(invocation -> null);

            mojo.execute();

            // Verify changed conflicts were detected and reported
            verify(log).info("⚠️  Transitive dependency conflict differences found between branches:");
            verify(log).info("📊 CHANGED CONFLICTS (diff between branches):");
            verify(log).info(contains("📋 SUMMARY:"));
            verify(log).info(contains("1 changed"));
        }
    }

    @Test
    void testNoConflictsDetected() throws Exception {
        // Setup: Create resolved artifacts
        Set<Artifact> resolvedArtifacts = new HashSet<>();
        resolvedArtifacts.add(createArtifact("com.example", "lib1", "1.1.0"));
        resolvedArtifacts.add(createArtifact("com.example", "lib2", "2.0.0"));
        when(project.getArtifacts()).thenReturn(resolvedArtifacts);

        // Create dependency graphs with no conflicts (same versions for both branches)
        DependencyNode baseBranchRoot = createDependencyGraph(
                createMockArtifact("com.example", "lib1", "1.1.0"),
                createMockArtifact("com.example", "lib2", "2.0.0"));

        DependencyNode currentBranchRoot = createDependencyGraph(
                createMockArtifact("com.example", "lib1", "1.1.0"),
                createMockArtifact("com.example", "lib2", "2.0.0"));

        CollectResult baseResult = mock(CollectResult.class);
        when(baseResult.getRoot()).thenReturn(baseBranchRoot);

        CollectResult currentResult = mock(CollectResult.class);
        when(currentResult.getRoot()).thenReturn(currentBranchRoot);

        when(repositorySystem.collectDependencies(eq(repoSession), any(CollectRequest.class)))
                .thenReturn(baseResult)
                .thenReturn(currentResult);

        try (MockedStatic<GitRepositoryUtil> gitUtil = mockStatic(GitRepositoryUtil.class)) {
            gitUtil.when(() -> GitRepositoryUtil.getRepository(any(File.class))).thenReturn(repository);
            gitUtil.when(() -> GitRepositoryUtil.getCurrentBranch(any(Git.class))).thenReturn("feature-branch");
            gitUtil.when(() -> GitRepositoryUtil.checkoutBranch(any(Git.class), anyString())).then(invocation -> null);

            mojo.execute();

            // Verify the analysis was performed
            verify(log).info("⏳ Analyzing dependency conflicts between 'develop' and 'feature-branch'");

            // Verify no conflicts message was displayed
            verify(log).info("🎉 No new conflicts found in feature branch!");

            // Verify no other conflict reporting occurred
            verify(log, never()).info("⚠️  Transitive dependency conflict differences found between branches:");
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

    private Artifact createArtifact(String groupId, String artifactId, String version) {
        return new DefaultArtifact(groupId, artifactId, version, "compile", "jar", "",
                new DefaultArtifactHandler("jar"));
    }

    /**
     * Creates a mock Aether artifact with the specified coordinates.
     */
    private org.eclipse.aether.artifact.Artifact createMockArtifact(String groupId, String artifactId, String version) {
        org.eclipse.aether.artifact.Artifact artifact = mock(org.eclipse.aether.artifact.Artifact.class);
        when(artifact.getGroupId()).thenReturn(groupId);
        when(artifact.getArtifactId()).thenReturn(artifactId);
        when(artifact.getVersion()).thenReturn(version);
        return artifact;
    }

    /**
     * Creates a mock dependency graph with the specified artifacts.
     * Sets up the dependency visitor pattern to traverse all provided artifacts.
     */
    private DependencyNode createDependencyGraph(org.eclipse.aether.artifact.Artifact... artifacts) {
        DependencyNode rootNode = mock(DependencyNode.class);
        when(rootNode.getArtifact()).thenReturn(null); // Root node typically has no artifact

        // Create child nodes for each artifact
        List<DependencyNode> childNodes = new ArrayList<>();
        for (org.eclipse.aether.artifact.Artifact artifact : artifacts) {
            DependencyNode childNode = mock(DependencyNode.class);
            when(childNode.getArtifact()).thenReturn(artifact);
            childNodes.add(childNode);
        }

        // Mock the visitor pattern to traverse all nodes
        doAnswer(invocation -> {
            org.eclipse.aether.graph.DependencyVisitor visitor = invocation.getArgument(0);

            // Visit root node
            visitor.visitEnter(rootNode);

            // Visit all child nodes
            for (DependencyNode childNode : childNodes) {
                visitor.visitEnter(childNode);
                visitor.visitLeave(childNode);
            }

            // Leave root node
            visitor.visitLeave(rootNode);
            return null;
        }).when(rootNode).accept(any(org.eclipse.aether.graph.DependencyVisitor.class));

        return rootNode;
    }
}
