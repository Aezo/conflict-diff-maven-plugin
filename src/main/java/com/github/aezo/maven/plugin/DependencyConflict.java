package com.github.aezo.maven.plugin;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a dependency conflict containing an artifact key and associated version conflicts.
 */
public class DependencyConflict {
    
    private String artifactKey;
    private Set<VersionConflict> conflicts;
    
    
    /**
     * Constructor with all fields.
     *
     * @param artifactKey the artifact key (e.g., groupId:artifactId)
     * @param conflicts   the set of version conflicts for this dependency
     * @throws IllegalArgumentException if artifactKey is null
     */
    public DependencyConflict(String artifactKey, Set<VersionConflict> conflicts) {
        if (artifactKey == null) {
            throw new IllegalArgumentException("ArtifactKey cannot be null");
        }
        this.artifactKey = artifactKey;
        this.conflicts = conflicts;
    }
    
    /**
     * Convenience constructor that accepts a Collection and converts it to a Set.
     *
     * @param artifactKey the artifact key (e.g., groupId:artifactId)
     * @param conflicts   the collection of version conflicts for this dependency
     * @throws IllegalArgumentException if artifactKey is null
     */
    public DependencyConflict(String artifactKey, Collection<VersionConflict> conflicts) {
        if (artifactKey == null) {
            throw new IllegalArgumentException("ArtifactKey cannot be null");
        }
        this.artifactKey = artifactKey;
        this.conflicts = conflicts != null ? new HashSet<>(conflicts) : null;
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
    public Set<VersionConflict> getConflicts() {
        return conflicts;
    }
    
    /**
     * Sets the set of version conflicts.
     *
     * @param conflicts the set of version conflicts to set
     */
    public void setConflicts(Set<VersionConflict> conflicts) {
        this.conflicts = conflicts;
    }
    
    /**
     * Adds the conflicts from another DependencyConflict to this one and returns a new DependencyConflict.
     * Matching version conflicts (same pomVersion/resolvedVersion) will have their counts added.
     *
     * @param other the DependencyConflict to add
     * @return a new DependencyConflict with combined conflicts
     * @throws IllegalArgumentException if artifactKey doesn't match or other is null
     */
    public DependencyConflict add(DependencyConflict other) {
        if (other == null) {
            throw new IllegalArgumentException("DependencyConflict cannot be null");
        }
        if (!Objects.equals(this.artifactKey, other.artifactKey)) {
            throw new IllegalArgumentException("ArtifactKey must be the same: " + this.artifactKey + " vs " + other.artifactKey);
        }
        
        // Create maps for efficient lookup during merging
        Map<VersionConflict, VersionConflict> thisMap = new HashMap<>();
        if (this.conflicts != null) {
            for (VersionConflict conflict : this.conflicts) {
                thisMap.put(conflict, conflict);
            }
        }
        
        Map<VersionConflict, VersionConflict> resultMap = new HashMap<>(thisMap);
        
        // Add or merge conflicts from other object
        if (other.conflicts != null) {
            for (VersionConflict otherConflict : other.conflicts) {
                VersionConflict existing = thisMap.get(otherConflict);
                if (existing != null) {
                    // Merge counts for existing conflict
                    resultMap.put(otherConflict, existing.add(otherConflict));
                } else {
                    // Add new conflict
                    resultMap.put(otherConflict, otherConflict);
                }
            }
        }
        
        return new DependencyConflict(this.artifactKey, new HashSet<>(resultMap.values()));
    }
    
    /**
     * Creates a union of conflicts from this and another DependencyConflict and returns a new DependencyConflict.
     * Matching version conflicts will have their counts added.
     *
     * @param other the DependencyConflict to union with
     * @return a new DependencyConflict with union of conflicts
     * @throws IllegalArgumentException if artifactKey doesn't match or other is null
     */
    public DependencyConflict union(DependencyConflict other) {
        // Union is the same as addition for this use case
        return this.add(other);
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
    public DependencyConflict diff(DependencyConflict base) {
        if (base == null) {
            throw new IllegalArgumentException("DependencyConflict cannot be null");
        }
        if (!Objects.equals(this.artifactKey, base.artifactKey)) {
            throw new IllegalArgumentException("ArtifactKey must be the same: " + this.artifactKey + " vs " + base.artifactKey);
        }
        
        Map<VersionConflict, VersionConflict> thisMap = new HashMap<>();
        if (this.conflicts != null) {
            for (VersionConflict conflict : this.conflicts) {
                thisMap.put(conflict, conflict);
            }
        }
        
        Map<VersionConflict, VersionConflict> otherMap = new HashMap<>();
        if (base.conflicts != null) {
            for (VersionConflict conflict : base.conflicts) {
                otherMap.put(conflict, conflict);
            }
        }
        
        Map<VersionConflict, VersionConflict> resultMap = new HashMap<>();
        
        // Process conflicts from this object
        if (this.conflicts != null) {
            for (VersionConflict thisConflict : this.conflicts) {
                VersionConflict otherConflict = otherMap.get(thisConflict);
                if (otherConflict != null) {
                    // Conflict exists in both - compute difference
                    VersionConflict diff = thisConflict.subtract(otherConflict);
                    resultMap.put(thisConflict, diff);
                } else {
                    // Conflict only in this - keep as positive
                    resultMap.put(thisConflict, thisConflict);
                }
            }
        }
        
        // Process conflicts from other that are not in this (will be negative)
        if (base.conflicts != null) {
            for (VersionConflict otherConflict : base.conflicts) {
                if (!thisMap.containsKey(otherConflict)) {
                    // Conflict only in other - make negative
                    VersionConflict negativeConflict = new VersionConflict(
                        otherConflict.getPomVersion(), 
                        otherConflict.getResolvedVersion(), 
                        -otherConflict.getCount()
                    );
                    resultMap.put(otherConflict, negativeConflict);
                }
            }
        }
        
        return new DependencyConflict(this.artifactKey, new HashSet<>(resultMap.values()));
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        
        DependencyConflict that = (DependencyConflict) obj;
        
        return Objects.equals(artifactKey, that.artifactKey) &&
               Objects.equals(conflicts, that.conflicts);
    }
    
    // Hashcode can be ignored
    
    @Override
    public String toString() {
        return "DependencyConflict{" +
                "artifactKey='" + artifactKey + '\'' +
                ", conflicts=" + conflicts +
                '}';
    }
}
