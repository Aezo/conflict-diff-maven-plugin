package com.github.aezo.maven.plugin.model;

import java.util.Objects;

import org.apache.maven.artifact.versioning.ComparableVersion;

/**
 * Represents a version conflict between a POM version and resolved version.
 */
public class VersionConflict {
    
    private ComparableVersion pomVersion;
    private ComparableVersion resolvedVersion;
    private int count;
    
    /**
     * Constructor with all fields.
     *
     * @param pomVersion      the version specified in the POM (cannot be null)
     * @param resolvedVersion the version that was actually resolved (cannot be null)
     * @param count          the count of occurrences
     * @throws IllegalArgumentException if pomVersion or resolvedVersion is null
     */
    public VersionConflict(ComparableVersion pomVersion, ComparableVersion resolvedVersion, int count) {
        if (pomVersion == null) {
            throw new IllegalArgumentException("pomVersion cannot be null");
        }
        if (resolvedVersion == null) {
            throw new IllegalArgumentException("resolvedVersion cannot be null");
        }
        this.pomVersion = pomVersion;
        this.resolvedVersion = resolvedVersion;
        this.count = count;
    }
    
    /**
     * Gets the POM version.
     *
     * @return the POM version
     */
    public ComparableVersion getPomVersion() {
        return pomVersion;
    }
    
    /**
     * Sets the POM version.
     *
     * @param pomVersion the POM version to set (cannot be null)
     * @throws IllegalArgumentException if pomVersion is null
     */
    public void setPomVersion(ComparableVersion pomVersion) {
        if (pomVersion == null) {
            throw new IllegalArgumentException("pomVersion cannot be null");
        }
        this.pomVersion = pomVersion;
    }
    
    /**
     * Gets the resolved version.
     *
     * @return the resolved version
     */
    public ComparableVersion getResolvedVersion() {
        return resolvedVersion;
    }
    
    /**
     * Sets the resolved version.
     *
     * @param resolvedVersion the resolved version to set (cannot be null)
     * @throws IllegalArgumentException if resolvedVersion is null
     */
    public void setResolvedVersion(ComparableVersion resolvedVersion) {
        if (resolvedVersion == null) {
            throw new IllegalArgumentException("resolvedVersion cannot be null");
        }
        this.resolvedVersion = resolvedVersion;
    }
    
    /**
     * Gets the count.
     *
     * @return the count
     */
    public int getCount() {
        return count;
    }
    
    /**
     * Sets the count.
     *
     * @param count the count to set
     */
    public void setCount(int count) {
        this.count = count;
    }
    
    /**
     * Adds the count from another VersionConflict to this one and returns a new VersionConflict.
     *
     * @param other the VersionConflict to add
     * @return a new VersionConflict with the sum of counts
     * @throws IllegalArgumentException if other is null or versions don't match
     */
    public VersionConflict add(VersionConflict other) {
        if (other == null) {
            throw new IllegalArgumentException("VersionConflict cannot be null");
        }
        
        // Check that pomVersion matches
        if (this.pomVersion == null) {
            if (other.pomVersion != null) {
                throw new IllegalArgumentException("POM versions must be the same: null vs " + other.pomVersion);
            }
        } else {
            if (!this.pomVersion.equals(other.pomVersion)) {
                throw new IllegalArgumentException("POM versions must be the same: " + this.pomVersion + " vs " + other.pomVersion);
            }
        }
        
        // Check that resolvedVersion matches
        if (this.resolvedVersion == null) {
            if (other.resolvedVersion != null) {
                throw new IllegalArgumentException("Resolved versions must be the same: null vs " + other.resolvedVersion);
            }
        } else {
            if (!this.resolvedVersion.equals(other.resolvedVersion)) {
                throw new IllegalArgumentException("Resolved versions must be the same: " + this.resolvedVersion + " vs " + other.resolvedVersion);
            }
        }
        
        return new VersionConflict(this.pomVersion, this.resolvedVersion, this.count + other.count);
    }
    
    /**
     * Subtracts the count from another VersionConflict from this one and returns a new VersionConflict.
     *
     * @param other the VersionConflict to subtract
     * @return a new VersionConflict with the difference of counts
     * @throws IllegalArgumentException if other is null or versions don't match
     */
    public VersionConflict subtract(VersionConflict other) {
        if (other == null) {
            throw new IllegalArgumentException("VersionConflict cannot be null");
        }
        
        // Check that pomVersion matches
        if (this.pomVersion == null) {
            if (other.pomVersion != null) {
                throw new IllegalArgumentException("POM versions must be the same: null vs " + other.pomVersion);
            }
        } else {
            if (!this.pomVersion.equals(other.pomVersion)) {
                throw new IllegalArgumentException("POM versions must be the same: " + this.pomVersion + " vs " + other.pomVersion);
            }
        }
        
        // Check that resolvedVersion matches
        if (this.resolvedVersion == null) {
            if (other.resolvedVersion != null) {
                throw new IllegalArgumentException("Resolved versions must be the same: null vs " + other.resolvedVersion);
            }
        } else {
            if (!this.resolvedVersion.equals(other.resolvedVersion)) {
                throw new IllegalArgumentException("Resolved versions must be the same: " + this.resolvedVersion + " vs " + other.resolvedVersion);
            }
        }
        
        return new VersionConflict(this.pomVersion, this.resolvedVersion, this.count - other.count);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        
        VersionConflict that = (VersionConflict) obj;
        
        // Count can be ignored

        if (pomVersion == null) {
            if (that.pomVersion != null) {
                return false;
            }
        } else {
            if (!pomVersion.equals(that.pomVersion)) {
                return false;
            }
        }
        
        if (resolvedVersion == null) {
            if (that.resolvedVersion != null) {
                return false;
            }
        } else {
            if (!resolvedVersion.equals(that.resolvedVersion)) {
                return false;
            }
        }
        
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pomVersion, resolvedVersion);
    }

    public String hashKey() {
        return pomVersion + "->" + resolvedVersion;
    }
    
    @Override
    public String toString() {
        return "VersionConflict{" +
                "pomVersion=" + pomVersion +
                ", resolvedVersion=" + resolvedVersion +
                ", count=" + count +
                '}';
    }
}
