package com.github.aezo.maven.plugin.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for VersionConflict.
 */
class VersionConflictTest {

    private VersionConflict conflict1;
    private VersionConflict conflict2;
    private VersionConflict conflict3;

    @BeforeEach
    void setUp() {
        conflict1 = new VersionConflict(new ComparableVersion("1.0.0"), new ComparableVersion("1.1.0"), 5);
        conflict2 = new VersionConflict(new ComparableVersion("1.0.0"), new ComparableVersion("1.1.0"), 3);
        conflict3 = new VersionConflict(new ComparableVersion("2.0.0"), new ComparableVersion("2.1.0"), 2);
    }

    @Test
    void testAdd() {
        VersionConflict result = conflict1.add(conflict2);

        assertThat(result.getPomVersion()).isEqualTo(conflict1.getPomVersion());
        assertThat(result.getResolvedVersion()).isEqualTo(conflict1.getResolvedVersion());
        assertThat(result.getCount()).isEqualTo(8); // 5 + 3
    }

    @Test
    void testAddWithDifferentPomVersionThrowsException() {
        VersionConflict otherConflict = new VersionConflict(new ComparableVersion("3.0.0"),
                new ComparableVersion("1.1.0"), 2);

        assertThatThrownBy(() -> conflict1.add(otherConflict))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("POM versions must be the same: 1.0.0 vs 3.0.0");
    }

    @Test
    void testAddWithDifferentResolvedVersionThrowsException() {
        VersionConflict otherConflict = new VersionConflict(new ComparableVersion("1.0.0"),
                new ComparableVersion("3.1.0"), 2);

        assertThatThrownBy(() -> conflict1.add(otherConflict))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Resolved versions must be the same: 1.1.0 vs 3.1.0");
    }

    @Test
    void testConstructorWithNullPomVersionThrowsException() {
        assertThatThrownBy(() -> new VersionConflict(null, new ComparableVersion("1.1.0"), 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("pomVersion cannot be null");
    }

    @Test
    void testConstructorWithNullResolvedVersionThrowsException() {
        assertThatThrownBy(() -> new VersionConflict(new ComparableVersion("1.0.0"), null, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("resolvedVersion cannot be null");
    }

    @Test
    void testConstructorWithBothNullVersionsThrowsException() {
        assertThatThrownBy(() -> new VersionConflict(null, null, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("pomVersion cannot be null");
    }

    @Test
    void testSetPomVersionWithNullThrowsException() {
        assertThatThrownBy(() -> conflict1.setPomVersion(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("pomVersion cannot be null");
    }

    @Test
    void testSetResolvedVersionWithNullThrowsException() {
        assertThatThrownBy(() -> conflict1.setResolvedVersion(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("resolvedVersion cannot be null");
    }

    @Test
    void testAddWithNullThrowsException() {
        assertThatThrownBy(() -> conflict1.add(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("VersionConflict cannot be null");
    }

    @Test
    void testSubtract() {
        VersionConflict result = conflict1.subtract(conflict2);

        assertThat(result.getPomVersion()).isEqualTo(conflict1.getPomVersion());
        assertThat(result.getResolvedVersion()).isEqualTo(conflict1.getResolvedVersion());
        assertThat(result.getCount()).isEqualTo(2); // 5 - 3
    }

    @Test
    void testSubtractWithDifferentPomVersionThrowsException() {
        VersionConflict otherConflict = new VersionConflict(new ComparableVersion("3.0.0"),
                new ComparableVersion("1.1.0"), 2);

        assertThatThrownBy(() -> conflict1.subtract(otherConflict))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("POM versions must be the same: 1.0.0 vs 3.0.0");
    }

    @Test
    void testSubtractWithDifferentResolvedVersionThrowsException() {
        VersionConflict otherConflict = new VersionConflict(new ComparableVersion("1.0.0"),
                new ComparableVersion("3.1.0"), 2);

        assertThatThrownBy(() -> conflict1.subtract(otherConflict))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Resolved versions must be the same: 1.1.0 vs 3.1.0");
    }

    @Test
    void testSubtractWithNullThrowsException() {
        assertThatThrownBy(() -> conflict1.subtract(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("VersionConflict cannot be null");
    }

    @Test
    void testSubtractResultingInNegativeCount() {
        VersionConflict result = conflict2.subtract(conflict1); // 3 - 5 = -2

        assertThat(result.getCount()).isEqualTo(-2);
    }

    @Test
    void testEqualsWithSameVersions() {
        VersionConflict other = new VersionConflict(new ComparableVersion("1.0.0"), new ComparableVersion("1.1.0"), 10);

        assertThat(conflict1).isEqualTo(other);
    }

    @Test
    void testEqualsWithDifferentVersions() {
        assertThat(conflict1).isNotEqualTo(conflict3);
    }

    @Test
    void testEqualsWithSameObject() {
        assertThat(conflict1).isEqualTo(conflict1);
    }

    @Test
    void testEqualsWithNull() {
        assertThat(conflict1).isNotEqualTo(null);
    }

    @Test
    void testEqualsWithDifferentClass() {
        assertThat(conflict1).isNotEqualTo("not a VersionConflict");
    }

    @Test
    void testToString() {
        String result = conflict1.toString();

        assertThat(result).contains("VersionConflict{");
        assertThat(result).contains("pomVersion=1.0.0");
        assertThat(result).contains("resolvedVersion=1.1.0");
        assertThat(result).contains("count=5");
    }

    @Test
    void testEqualsIgnoresCount() {
        VersionConflict conflict1 = new VersionConflict(new ComparableVersion("1.0.0"), new ComparableVersion("1.1.0"),
                5);
        VersionConflict conflict2 = new VersionConflict(new ComparableVersion("1.0.0"), new ComparableVersion("1.1.0"),
                999);

        // Equals should ignore count as per the implementation
        assertThat(conflict1).isEqualTo(conflict2);
    }
}
