package com.github.aezo.maven.plugin.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.aezo.maven.plugin.model.DependencyConflict;
import com.github.aezo.maven.plugin.model.VersionConflict;

/**
 * Unit tests for MavenTreeConflictDetectionStrategy.
 */
@ExtendWith(MockitoExtension.class)
class MavenTreeConflictDetectionStrategyTest {

    @Mock
    private MavenProject mockProject;

    @Mock
    private Consumer<String> mockDebugLogger;

    private MavenTreeConflictDetectionStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new MavenTreeConflictDetectionStrategy(mockProject, mockDebugLogger);
    }

    @Test
    void testParseConflicts_withConflictLines() throws Exception {
        // Given - sample Maven dependency tree output with conflicts
        List<String> output = Arrays.asList(
            "[INFO] com.example:test-project:jar:1.0.0",
            "[INFO] +- org.springframework:spring-core:jar:5.3.21:compile",
            "[INFO] |  \\- (org.springframework:spring-jcl:jar:5.3.21:compile - omitted for conflict with 5.3.20)",
            "[INFO] +- org.apache.commons:commons-lang3:jar:3.12.0:compile",
            "[INFO] \\- (junit:junit:jar:4.13.1:test - omitted for conflict with 4.13.2)",
            "[INFO] ------------------------------------------------------------------------"
        );

        // When - parse conflicts using reflection to access private method
        Method parseConflictsMethod = MavenTreeConflictDetectionStrategy.class
                .getDeclaredMethod("parseConflicts", List.class);
        parseConflictsMethod.setAccessible(true);

        VersionConflict junitConflictExpected = new VersionConflict(new ComparableVersion("4.13.1"), new ComparableVersion("4.13.2"), 1);
        VersionConflict springConflictExpected = new VersionConflict(new ComparableVersion("5.3.21"), new ComparableVersion("5.3.20"), 1);
        
        @SuppressWarnings("unchecked")
        Map<String, DependencyConflict> conflictMap = (Map<String, DependencyConflict>) 
                parseConflictsMethod.invoke(strategy, output);

        // Then - should find two conflicts
        assertThat(conflictMap).hasSize(2);
        
        // Verify spring-jcl conflict
        assertThat(conflictMap).containsKey("org.springframework:spring-jcl");
        DependencyConflict springConflicts = conflictMap.get("org.springframework:spring-jcl");
        assertThat(springConflicts.getConflicts()).hasSize(1);
        VersionConflict springConflict = springConflicts.getConflicts().get(springConflictExpected.hashKey());
        assertEquals(springConflict, springConflictExpected);
        
        // Verify junit conflict
        assertThat(conflictMap).containsKey("junit:junit");
        DependencyConflict junitConflicts = conflictMap.get("junit:junit");
        assertThat(junitConflicts.getConflicts()).hasSize(1);
        VersionConflict junitConflict = junitConflicts.getConflicts().get(junitConflictExpected.hashKey());
        assertEquals(junitConflict, junitConflictExpected);
    }

    @Test
    void testParseConflicts_withNoConflicts() throws Exception {
        // Given - Maven output with no conflicts
        List<String> output = Arrays.asList(
            "[INFO] com.example:test-project:jar:1.0.0",
            "[INFO] +- org.springframework:spring-core:jar:5.3.21:compile",
            "[INFO] +- org.apache.commons:commons-lang3:jar:3.12.0:compile",
            "[INFO] \\- junit:junit:jar:4.13.2:test",
            "[INFO] ------------------------------------------------------------------------"
        );

        // When - parse conflicts
        Method parseConflictsMethod = MavenTreeConflictDetectionStrategy.class
                .getDeclaredMethod("parseConflicts", List.class);
        parseConflictsMethod.setAccessible(true);
        
        @SuppressWarnings("unchecked")
        Map<String, List<VersionConflict>> conflictMap = (Map<String, List<VersionConflict>>) 
                parseConflictsMethod.invoke(strategy, output);

        // Then - should find no conflicts
        assertThat(conflictMap).isEmpty();
    }
}
