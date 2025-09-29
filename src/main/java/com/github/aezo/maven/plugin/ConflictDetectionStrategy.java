package com.github.aezo.maven.plugin;

import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.resolution.DependencyResolutionException;

/**
 * Strategy interface for detecting transitive dependency conflicts.
 * 
 * This interface defines the contract for different strategies that can be used
 * to collect and analyze transitive dependency conflicts in Maven projects.
 */
public interface ConflictDetectionStrategy {
    
    /**
     * Collects transitive dependency conflicts for a given branch.
     * 
     * @param branchName the name of the branch to analyze
     * @return a list of dependency conflicts found in the branch
     * @throws DependencyCollectionException if dependency collection fails
     * @throws DependencyResolutionException if dependency resolution fails
     * @throws MojoExecutionException if the analysis fails
     */
    List<DependencyConflict> collectTransitiveDependencyConflicts(String branchName)
            throws DependencyCollectionException, DependencyResolutionException, MojoExecutionException;
}
