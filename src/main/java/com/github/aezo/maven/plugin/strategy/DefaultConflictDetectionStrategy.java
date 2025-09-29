package com.github.aezo.maven.plugin.strategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.artifact.JavaScopes;

import com.github.aezo.maven.plugin.model.DependencyConflict;
import com.github.aezo.maven.plugin.model.VersionConflict;

/**
 * Default implementation of the conflict detection strategy.
 * 
 * This implementation analyzes transitive dependency conflicts by building
 * dependency
 * graphs using Maven's internal APIs and identifying cases where transitive
 * dependencies are overridden due to version conflicts.
 */
public class DefaultConflictDetectionStrategy implements ConflictDetectionStrategy {

    private final MavenProject project;
    private final RepositorySystem repositorySystem;
    private final RepositorySystemSession repoSession;
    private final List<RemoteRepository> remoteRepos;
    private final Consumer<String> debugLogger;

    public DefaultConflictDetectionStrategy(MavenProject project,
            RepositorySystem repositorySystem,
            RepositorySystemSession repoSession,
            List<RemoteRepository> remoteRepos,
            Consumer<String> debugLogger) {
        this.project = project;
        this.repositorySystem = repositorySystem;
        this.repoSession = repoSession;
        this.remoteRepos = remoteRepos;
        this.debugLogger = debugLogger;
    }

    @Override
    public List<DependencyConflict> collectTransitiveDependencyConflicts(String branchName) {
        try {
            debugLogger.accept("⏳ Collecting transitive dependency conflicts for branch: " + branchName);

            // Build the dependency graph using Maven Resolver
            CollectRequest collectRequest = new CollectRequest();

            // Convert Maven Artifact to Aether Artifact
            Artifact projectArtifact = project.getArtifact();
            org.eclipse.aether.artifact.Artifact aetherArtifact = new DefaultArtifact(
                    projectArtifact.getGroupId(),
                    projectArtifact.getArtifactId(),
                    projectArtifact.getClassifier(),
                    projectArtifact.getType(),
                    projectArtifact.getVersion());

            // COMPILE scope is used for conflict detection.
            collectRequest.setRoot(new Dependency(aetherArtifact, JavaScopes.COMPILE));
            collectRequest.setRepositories(remoteRepos);

            DependencyNode rootNode = repositorySystem.collectDependencies(repoSession, collectRequest).getRoot();

            // Extract conflict information from the dependency graph
            List<DependencyConflict> dependencyConflicts = extractConflicts(rootNode);

            debugLogger
                    .accept("Found " + dependencyConflicts.size() + " dependency conflicts for branch: " + branchName);

            // Return sorted list of conflict information for comparison
            return dependencyConflicts;
        } catch (DependencyCollectionException | MojoExecutionException e) {
            throw new RuntimeException("Failed to collect transitive dependency conflicts for branch: " + branchName,
                    e);
        }
    }

    /**
     * Extracts dependency conflict information from the dependency graph.
     * A conflict occurs when multiple versions of the same artifact exist in the
     * graph
     * and Maven's conflict resolution chooses one version over others.
     * 
     * @param rootNode  The root node of the dependency graph
     * @param conflicts The set to collect conflict descriptions
     */
    private List<DependencyConflict> extractConflicts(DependencyNode rootNode) throws MojoExecutionException {
        // Collect all artifact versions with counts from the dependency graph
        Map<String, Map<ComparableVersion, Integer>> artifactVersions = collectArtifactVersions(rootNode);

        // Create a resolved artifacts versions map
        Map<String, ComparableVersion> resolvedArtifactsVersions = new HashMap<>();
        for (Artifact resolvedArtifact : project.getArtifacts()) {
            resolvedArtifactsVersions.put(resolvedArtifact.getGroupId() + ":" + resolvedArtifact.getArtifactId(),
                    new ComparableVersion(resolvedArtifact.getVersion()));
        }

        List<DependencyConflict> dependencyConflicts = new ArrayList<>();

        // Process artifacts with version conflicts
        for (Map.Entry<String, Map<ComparableVersion, Integer>> entry : artifactVersions.entrySet()) {
            String artifactKey = entry.getKey();
            Map<ComparableVersion, Integer> versionCounts = entry.getValue();

            // Check if this artifact has version conflicts (multiple versions)
            if (versionCounts.size() > 1) {
                // Multiple versions found - this indicates a conflict
                List<VersionConflict> versionConflicts = computeVersionConflicts(artifactKey, versionCounts,
                        resolvedArtifactsVersions);
                if (!versionConflicts.isEmpty()) {
                    dependencyConflicts.add(new DependencyConflict(artifactKey, versionConflicts));
                }
            }
        }

        // Return all dependency conflicts found
        debugLogger.accept("Found " + dependencyConflicts.size() + " artifacts with version conflicts");
        return dependencyConflicts;
    }

    /**
     * Traverses the dependency graph and collects all artifact versions with their
     * counts.
     * Returns a map of artifact keys to maps of ComparableVersion objects and their
     * occurrence counts.
     * 
     * @param rootNode The root node of the dependency graph
     * @return Map of artifact keys to maps of ComparableVersion objects and their
     *         counts
     */
    private Map<String, Map<ComparableVersion, Integer>> collectArtifactVersions(DependencyNode rootNode) {
        // Map to collect all versions of each artifact found in the graph
        Map<String, List<DependencyNode>> artifactVersions = new HashMap<>();

        // Traverse the entire dependency graph to collect all artifacts
        rootNode.accept(new DependencyVisitor() {
            @Override
            public boolean visitEnter(DependencyNode node) {
                if (node.getArtifact() != null) {
                    String artifactKey = node.getArtifact().getGroupId() + ":" + node.getArtifact().getArtifactId();
                    artifactVersions.computeIfAbsent(artifactKey, k -> new ArrayList<>()).add(node);
                }
                return true;
            }

            @Override
            public boolean visitLeave(DependencyNode node) {
                return true;
            }
        });

        Map<String, Map<ComparableVersion, Integer>> artifactVersionsWithCounts = new HashMap<>();

        for (Map.Entry<String, List<DependencyNode>> entry : artifactVersions.entrySet()) {
            List<DependencyNode> versions = entry.getValue();

            // Count occurrences of each version
            Map<ComparableVersion, Integer> versionCounts = versions.stream()
                    .map(node -> new ComparableVersion(node.getArtifact().getVersion()))
                    .collect(Collectors.groupingBy(
                            version -> version,
                            Collectors.summingInt(version -> 1)));

            artifactVersionsWithCounts.put(entry.getKey(), versionCounts);
        }

        return artifactVersionsWithCounts;
    }

    /**
     * Analyzes a version conflict for a specific artifact and generates a conflict
     * description.
     * Separates conflicts into upgrades (less problematic) and downgrades (more
     * problematic).
     * 
     * @param artifactKey   The artifact identifier (groupId:artifactId)
     * @param versionCounts Map of ComparableVersion objects to their occurrence
     *                      counts
     * @return List of version conflicts for the artifact
     * @throws MojoExecutionException if no winning version can be found for the
     *                                artifact
     */
    private List<VersionConflict> computeVersionConflicts(String artifactKey,
            Map<ComparableVersion, Integer> versionCounts,
            Map<String, ComparableVersion> resolvedArtifactsVersions) throws MojoExecutionException {
        // Find the winning version (the one that appears in the resolved dependencies)
        ComparableVersion winningVersion = findWinningVersionFromProject(artifactKey, resolvedArtifactsVersions);

        // Separate conflicted versions into upgrades and downgrades
        List<VersionConflict> versionConflicts = new ArrayList<>();

        for (Map.Entry<ComparableVersion, Integer> versionEntry : versionCounts.entrySet()) {
            ComparableVersion version = versionEntry.getKey();
            Integer count = versionEntry.getValue();

            if (!version.equals(winningVersion)) {
                VersionConflict versionConflict = new VersionConflict(version, winningVersion, count);

                versionConflicts.add(versionConflict);
            }
        }

        return versionConflicts;
    }

    /**
     * Finds the winning version of an artifact from the resolved project
     * dependencies.
     * 
     * @param artifactKey               The artifact identifier (groupId:artifactId)
     * @param resolvedArtifactsVersions Map of resolved artifact versions
     * @return The resolved version for the artifact
     * @throws MojoExecutionException if the artifact is not found in resolved
     *                                dependencies
     */
    private ComparableVersion findWinningVersionFromProject(String artifactKey,
            Map<String, ComparableVersion> resolvedArtifactsVersions) throws MojoExecutionException {
        if (resolvedArtifactsVersions.containsKey(artifactKey)) {
            return resolvedArtifactsVersions.get(artifactKey);
        }

        throw new MojoExecutionException("❌ No winning version found for artifact: " + artifactKey +
                ". This indicates an inconsistency in the dependency resolution process.");
    }
}
