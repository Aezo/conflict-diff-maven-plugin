package com.github.aezo.maven.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for DependencyConflict.
 */
class DependencyConflictTest {

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

        dependency1 = new DependencyConflict("com.example:library1", Arrays.asList(version1, version2));
        dependency2 = new DependencyConflict("com.example:library1", Arrays.asList(version3));
        dependency3 = new DependencyConflict("com.example:library2", Arrays.asList(version1));
    }

    @Test
    void testConstructorWithNullArtifactKeyThrowsException() {
        List<VersionConflict> conflicts = Arrays.asList(version1, version2);

        assertThatThrownBy(() -> new DependencyConflict(null, conflicts))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("ArtifactKey cannot be null");
    }

    @Test
    void testAdd() {
        DependencyConflict result = dependency1.add(dependency2);

        assertThat(result.getArtifactKey()).isEqualTo("com.example:library1");
        assertThat(result.getConflicts()).hasSize(2);

        ComparableVersion mergedVersion = new ComparableVersion("1.0");
        
        // Find the merged version conflict (1.0 -> 1.1)
        VersionConflict merged = result.getConflicts().stream()
            .filter(vc -> vc.getPomVersion().equals(mergedVersion))
            .findFirst()
            .orElse(null);
        
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
    void testUnion() {
        DependencyConflict result = dependency1.union(dependency2);

        // Union should be the same as add
        DependencyConflict addResult = dependency1.add(dependency2);
        assertThat(result.getArtifactKey()).isEqualTo(addResult.getArtifactKey());
        assertThat(result.getConflicts()).hasSameSizeAs(addResult.getConflicts());
    }

    @Test
    void testDiff() {
        // Create test conflicts for diff
        VersionConflict baseVersion1 = new VersionConflict(new ComparableVersion("1.0"), new ComparableVersion("1.1"), 2);
        VersionConflict baseVersion2 = new VersionConflict(new ComparableVersion("2.0"), new ComparableVersion("2.1"), 1);
        VersionConflict currentVersion1 = new VersionConflict(new ComparableVersion("1.0"), new ComparableVersion("1.1"), 5);
        VersionConflict currentVersion3 = new VersionConflict(new ComparableVersion("3.0"), new ComparableVersion("3.1"), 2);

        DependencyConflict current = new DependencyConflict("com.example:library", 
            Arrays.asList(currentVersion1, currentVersion3));
        DependencyConflict base = new DependencyConflict("com.example:library", 
            Arrays.asList(baseVersion1, baseVersion2));

        DependencyConflict diff = current.diff(base);

        assertThat(diff.getArtifactKey()).isEqualTo("com.example:library");
        assertThat(diff.getConflicts()).hasSize(3);

        // Check for increased conflict (1.0 -> 1.1): 5 - 2 = 3
        VersionConflict increasedConflict = diff.getConflicts().stream()
            .filter(vc -> vc.getPomVersion().equals(new ComparableVersion("1.0")))
            .findFirst()
            .orElse(null);
        assertThat(increasedConflict).isNotNull();
        assertThat(increasedConflict.getCount()).isEqualTo(3);

        // Check for new conflict (3.0 -> 3.1): 2
        VersionConflict newConflict = diff.getConflicts().stream()
            .filter(vc -> vc.getPomVersion().equals(new ComparableVersion("3.0")))
            .findFirst()
            .orElse(null);
        assertThat(newConflict).isNotNull();
        assertThat(newConflict.getCount()).isEqualTo(2);

        // Check for removed conflict (2.0 -> 2.1): -1
        VersionConflict removedConflict = diff.getConflicts().stream()
            .filter(vc -> vc.getPomVersion().equals(new ComparableVersion("2.0")))
            .findFirst()
            .orElse(null);
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
        for (VersionConflict conflict : diff.getConflicts()) {
            assertThat(conflict.getCount()).isEqualTo(0);
        }
    }

    @Test
    void testEquals() {
        DependencyConflict other = new DependencyConflict("com.example:library1", Arrays.asList(version1, version2));

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
    void testAddWithNullConflicts() {
        DependencyConflict depWithNullConflicts = new DependencyConflict("com.example:library1", null);
        DependencyConflict result = depWithNullConflicts.add(dependency2);

        assertThat(result.getConflicts()).hasSize(1);
        assertThat(result.getConflicts().iterator().next()).isEqualTo(version3);
    }

    @Test
    void testDiffWithEmptyConflicts() {
        DependencyConflict empty = new DependencyConflict("com.example:library1", new ArrayList<>());
        DependencyConflict result = empty.diff(dependency2);

        assertThat(result.getConflicts()).hasSize(1);
        assertThat(result.getConflicts().iterator().next().getCount()).isEqualTo(-2); // Negative count for removed conflict
    }
}
