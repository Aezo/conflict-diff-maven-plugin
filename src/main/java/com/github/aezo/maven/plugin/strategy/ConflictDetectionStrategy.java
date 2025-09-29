package com.github.aezo.maven.plugin.strategy;

import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.resolution.DependencyResolutionException;

import com.github.aezo.maven.plugin.model.DependencyConflict;

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
     */
    List<DependencyConflict> collectTransitiveDependencyConflicts(String branchName);
}
