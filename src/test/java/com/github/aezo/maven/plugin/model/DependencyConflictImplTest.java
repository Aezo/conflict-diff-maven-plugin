package com.github.aezo.maven.plugin.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.aezo.maven.plugin.model.DependencyConflict;
import com.github.aezo.maven.plugin.model.DependencyConflictImpl;
import com.github.aezo.maven.plugin.model.VersionConflict;

/**
 * Unit tests for DependencyConflict.
 */
class DependencyConflictImplTest {

    private DependencyConflict dependency1;
    private DependencyConflict dependency2;
    private DependencyConflict dependency3;
    private VersionConflict version1;
    private VersionConflict version2;
    private VersionConflict version3;

    @BeforeEach
    void setUp() {
        version1 = new VersionConflict(new ComparableVersion("1.0"), new ComparableVersion("1.1"), 5);
        version2 = new VersionConflict(new ComparableVersion("2.0"), new ComparableVersion("2.1"), 3);
        version3 = new VersionConflict(new ComparableVersion("1.0"), new ComparableVersion("1.1"), 2);

        dependency1 = new DependencyConflictImpl("com.example:library1");
        dependency1.addConflict(version1);
        dependency1.addConflict(version2);
        dependency2 = new DependencyConflictImpl("com.example:library1");
        dependency2.addConflict(version3);
        dependency3 = new DependencyConflictImpl("com.example:library2");
        dependency3.addConflict(version1);
    }

    @Test
    void testConstructorWithNullArtifactKeyThrowsException() {
        assertThatThrownBy(() -> new DependencyConflictImpl(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("ArtifactKey cannot be null");
    }

    @Test
    void testAddConflict() {
        dependency1.addConflict(version3);

        assertThat(dependency1.getConflicts()).hasSize(2);
        assertThat(dependency1.getConflicts().get(version3.hashKey())).isNotNull();
        assertThat(dependency1.getConflicts().get(version3.hashKey()).getCount()).isEqualTo(7);
    }

    @Test
    void testAdd() {
        dependency1.add(dependency2);

        assertThat(dependency1.getArtifactKey()).isEqualTo("com.example:library1");
        assertThat(dependency1.getConflicts()).hasSize(2);
        
        // Find the merged version conflict (1.0 -> 1.1)
        VersionConflict merged = dependency1.getConflicts().get(version1.hashKey());
        
        assertThat(merged).isNotNull();
        assertThat(merged.getCount()).isEqualTo(7); // 5 + 2
    }

    @Test
    void testAddWithNullThrowsException() {
        assertThatThrownBy(() -> dependency1.add(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("DependencyConflict cannot be null");
    }

    @Test
    void testAddWithDifferentArtifactKeyThrowsException() {
        assertThatThrownBy(() -> dependency1.add(dependency3))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("ArtifactKey must be the same: com.example:library1 vs com.example:library2");
    }

    @Test
    void testDiff() {
        // Create test conflicts for diff
        VersionConflict baseVersion1 = new VersionConflict(new ComparableVersion("1.0"), new ComparableVersion("1.1"), 2);
        VersionConflict baseVersion2 = new VersionConflict(new ComparableVersion("2.0"), new ComparableVersion("2.1"), 1);
        VersionConflict currentVersion1 = new VersionConflict(new ComparableVersion("1.0"), new ComparableVersion("1.1"), 5);
        VersionConflict currentVersion3 = new VersionConflict(new ComparableVersion("3.0"), new ComparableVersion("3.1"), 2);

        /*
         * com.example:library
         * 1.0 -> 1.1: 5
         * 3.0 -> 3.1: 2
         */
        DependencyConflict current = new DependencyConflictImpl("com.example:library");
        current.addConflict(currentVersion1);
        current.addConflict(currentVersion3);
        
        /*
         * com.example:library
         * 1.0 -> 1.1: 2
         * 2.0 -> 2.1: 1
         */
        DependencyConflict base = new DependencyConflictImpl("com.example:library");
        base.addConflict(baseVersion1);
        base.addConflict(baseVersion2);

        DependencyConflict diff = current.diff(base);

        assertThat(diff.getArtifactKey()).isEqualTo("com.example:library");
        assertThat(diff.getConflicts()).hasSize(3);

        // Check for increased conflict (1.0 -> 1.1): 5 - 2 = 3
        VersionConflict increasedConflict = diff.getConflicts().get(baseVersion1.hashKey());
        assertThat(increasedConflict).isNotNull();
        assertThat(increasedConflict.getCount()).isEqualTo(3);

        // Check for new conflict (3.0 -> 3.1): 2
        VersionConflict newConflict = diff.getConflicts().get(currentVersion3.hashKey());
        assertThat(newConflict).isNotNull();
        assertThat(newConflict.getCount()).isEqualTo(2);

        // Check for removed conflict (2.0 -> 2.1): -1
        VersionConflict removedConflict = diff.getConflicts().get(baseVersion2.hashKey());
        assertThat(removedConflict).isNotNull();
        assertThat(removedConflict.getCount()).isEqualTo(-1);
    }

    @Test
    void testDiffWithNullThrowsException() {
        assertThatThrownBy(() -> dependency1.diff(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("DependencyConflict cannot be null");
    }

    @Test
    void testDiffWithDifferentArtifactKeyThrowsException() {
        assertThatThrownBy(() -> dependency1.diff(dependency3))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("ArtifactKey must be the same: com.example:library1 vs com.example:library2");
    }

    @Test
    void testDiffWithSameConflictsReturnsZeroCounts() {
        DependencyConflict diff = dependency1.diff(dependency1);

        assertThat(diff.getArtifactKey()).isEqualTo(dependency1.getArtifactKey());
        for (VersionConflict conflict : diff.getConflicts().values()) {
            assertThat(conflict.getCount()).isEqualTo(0);
        }
    }

    @Test
    void testEquals() {
        DependencyConflict other = new DependencyConflictImpl("com.example:library1");
        other.addConflict(version1);
        other.addConflict(version2);

        assertThat(dependency1).isEqualTo(other);
    }

    @Test
    void testEqualsWithDifferentArtifactKey() {
        assertThat(dependency1).isNotEqualTo(dependency3);
    }

    @Test
    void testEqualsWithSameObject() {
        assertThat(dependency1).isEqualTo(dependency1);
    }

    @Test
    void testEqualsWithNull() {
        assertThat(dependency1).isNotEqualTo(null);
    }

    @Test
    void testEqualsWithDifferentClass() {
        assertThat(dependency1).isNotEqualTo("not a DependencyConflict");
    }

    @Test
    void testToString() {
        String result = dependency1.toString();

        assertThat(result).contains("DependencyConflict{");
        assertThat(result).contains("artifactKey='com.example:library1'");
        assertThat(result).contains("conflicts=");
    }

    @Test
    void testDiffWithEmptyConflicts() {
        DependencyConflict dependency1 = new DependencyConflictImpl("com.example:library1");
        DependencyConflict diff = dependency1.diff(dependency2);

        assertThat(diff.getConflicts()).hasSize(1);
        assertThat(diff.getConflicts().values().iterator().next().getCount()).isEqualTo(-2); // Negative count for removed conflict
    }
}
