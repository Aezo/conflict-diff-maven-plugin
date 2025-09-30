package com.github.aezo.maven.plugin.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a dependency conflict containing an artifact key and associated version conflicts.
 */
public class DependencyConflictImpl implements DependencyConflict {
    
    private String artifactKey;
    private Map<String, VersionConflict> conflicts;
    
    /**
     * Constructor with artifact key.
     *
     * @param artifactKey the artifact key (e.g., groupId:artifactId)
     * @throws IllegalArgumentException if artifactKey is null
     */
    public DependencyConflictImpl(String artifactKey) {
        if (artifactKey == null) {
            throw new IllegalArgumentException("ArtifactKey cannot be null");
        }
        this.artifactKey = artifactKey;
        this.conflicts = new HashMap<>();
    }

    /**
     * Constructor with all fields.
     *
     * @param artifactKey the artifact key (e.g., groupId:artifactId)
     * @param conflicts   the set of version conflicts for this dependency
     * @throws IllegalArgumentException if artifactKey is null
     */
    public DependencyConflictImpl(String artifactKey, VersionConflict conflict) {
        if (artifactKey == null) {
            throw new IllegalArgumentException("ArtifactKey cannot be null");
        }
        this.artifactKey = artifactKey;
        this.conflicts = new HashMap<>();
        this.conflicts.put(conflict.hashKey(), conflict);
    }
    
    /**
     * Gets the artifact key.
     *
     * @return the artifact key
     */
    public String getArtifactKey() {
        return artifactKey;
    }
    
    /**
     * Sets the artifact key.
     *
     * @param artifactKey the artifact key to set
     */
    public void setArtifactKey(String artifactKey) {
        this.artifactKey = artifactKey;
    }
    
    /**
     * Gets the set of version conflicts.
     *
     * @return the set of version conflicts
     */
    public Map<String, VersionConflict> getConflicts() {
        return conflicts;
    }

    /**
     * Adds the count to another VersionConflict from this one.
     *
     * @param conflict the VersionConflict to add
     * @throws IllegalArgumentException if conflict is null
     */
    public void addConflict(VersionConflict conflict) {
        if (conflict.getCount() == 0) {
            return;
        }

        if (this.conflicts.containsKey(conflict.hashKey())) {
            this.conflicts.get(conflict.hashKey()).add(conflict);
            if (this.conflicts.get(conflict.hashKey()).getCount() == 0) {
                this.conflicts.remove(conflict.hashKey());
            }
        } else {
            this.conflicts.put(conflict.hashKey(), conflict);
        }
    }

    /**
     * Subtracts the count from another VersionConflict from this one.
     *
     * @param conflict the VersionConflict to subtract
     * @throws IllegalArgumentException if conflict is null
     */
    public void subtractConflict(VersionConflict conflict) {
        if (conflict.getCount() == 0) {
            return;
        }

        if (this.conflicts.containsKey(conflict.hashKey())) {
            this.conflicts.get(conflict.hashKey()).subtract(conflict);
            if (this.conflicts.get(conflict.hashKey()).getCount() == 0) {
                this.conflicts.remove(conflict.hashKey());
            }
        } else {
            this.conflicts.put(conflict.hashKey(), new VersionConflict(conflict.getPomVersion(), conflict.getResolvedVersion(), -conflict.getCount()));
        }
    }
    
    /**
     * Adds the conflicts from another DependencyConflict to this one.
     * Matching version conflicts (same pomVersion/resolvedVersion) will have their counts added.
     *
     * @param other the DependencyConflict to add
     * @return a new DependencyConflict with combined conflicts
     * @throws IllegalArgumentException if artifactKey doesn't match or other is null
     */
    public void add(DependencyConflict other) {
        if (other == null) {
            throw new IllegalArgumentException("DependencyConflict cannot be null");
        }
        if (!Objects.equals(this.artifactKey, other.getArtifactKey())) {
            throw new IllegalArgumentException("ArtifactKey must be the same: " + this.artifactKey + " vs " + other.getArtifactKey());
        }
        
        // Add or merge conflicts from other object
        if (other.getConflicts() != null) {
            for (Map.Entry<String, VersionConflict> entry : other.getConflicts().entrySet()) {
                this.addConflict(entry.getValue());
            }
        }
    }
    
    /**
     * Creates a union of conflicts from this and another DependencyConflict and returns a new DependencyConflict.
     * Matching version conflicts will have their counts added.
     *
     * @param other the DependencyConflict to union with
     * @return a new DependencyConflict with union of conflicts
     * @throws IllegalArgumentException if artifactKey doesn't match or other is null
     */
    public void union(DependencyConflict other) {
        // Union is the same as addition for this use case
        this.add(other);
    }
    
    /**
     * Computes a diff between this and another DependencyConflict and returns a new DependencyConflict.
     * The result shows the differences at the count level:
     * - Conflicts only in this: positive count
     * - Conflicts only in other: negative count  
     * - Conflicts in both: difference of counts (this.count - other.count)
     * 
     * <h3>Usage Examples:</h3>
     * <pre>{@code
     * // Example 1: Basic diff showing added conflicts
     * DependencyConflict current = new DependencyConflict("com.example:library", Arrays.asList(
     *     new VersionConflict("2.0", "2.1", 5),
     *     new VersionConflict("1.0", "1.2", 3)
     * ));
     * DependencyConflict base = new DependencyConflict("com.example:library", Arrays.asList(
     *     new VersionConflict("2.0", "2.1", 2)
     * ));
     * DependencyConflict diff = current.diff(base);
     * // Result: [VersionConflict("2.0", "2.1", 3), VersionConflict("1.0", "1.2", 3)]
     * //         First conflict shows increase of 3, second is new conflict
     * 
     * // Example 2: Diff showing removed conflicts (negative counts)
     * DependencyConflict current = new DependencyConflict("com.example:library", Arrays.asList(
     *     new VersionConflict("2.0", "2.1", 1)
     * ));
     * DependencyConflict base = new DependencyConflict("com.example:library", Arrays.asList(
     *     new VersionConflict("2.0", "2.1", 4),
     *     new VersionConflict("1.0", "1.2", 2)
     * ));
     * DependencyConflict diff = current.diff(base);
     * // Result: [VersionConflict("2.0", "2.1", -3), VersionConflict("1.0", "1.2", -2)]
     * //         First conflict decreased by 3, second was completely removed
     * 
     * // Example 3: Empty diff when no changes
     * DependencyConflict current = new DependencyConflict("com.example:library", Arrays.asList(
     *     new VersionConflict("2.0", "2.1", 5)
     * ));
     * DependencyConflict base = new DependencyConflict("com.example:library", Arrays.asList(
     *     new VersionConflict("2.0", "2.1", 5)
     * ));
     * DependencyConflict diff = current.diff(base);
     * // Result: [VersionConflict("2.0", "2.1", 0)]
     * //         No net change in conflict counts
     * }</pre>
     * 
     * @param base the DependencyConflict to diff against
     * @return a new DependencyConflict with the diff (counts can be negative)
     * @throws IllegalArgumentException if artifactKey doesn't match or base is null
     */
    public DependencyConflictImpl diff(DependencyConflict base) {
        if (base == null) {
            throw new IllegalArgumentException("DependencyConflict cannot be null");
        }
        if (!Objects.equals(this.artifactKey, base.getArtifactKey())) {
            throw new IllegalArgumentException("ArtifactKey must be the same: " + this.artifactKey + " vs " + base.getArtifactKey());
        }
        
        DependencyConflictImpl result = new DependencyConflictImpl(this.artifactKey);
        
        // Process conflicts from this object
        if (this.conflicts != null) {
            for (Map.Entry<String, VersionConflict> entry : this.conflicts.entrySet()) {
                result.addConflict(entry.getValue());
            }
        }
        
        // Process conflicts from other that are not in this (will be negative)
        if (base.getConflicts() != null) {
            for (Map.Entry<String, VersionConflict> entry : base.getConflicts().entrySet()) {
                result.subtractConflict(entry.getValue());
            }
        }
        
        return result;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        
        DependencyConflictImpl that = (DependencyConflictImpl) obj;
        
        return Objects.equals(artifactKey, that.artifactKey) &&
               Objects.equals(conflicts, that.conflicts);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(artifactKey);
    }
    
    @Override
    public String toString() {
        return "DependencyConflict{" +
                "artifactKey='" + artifactKey + '\'' +
                ", conflicts=" + conflicts +
                '}';
    }
}
