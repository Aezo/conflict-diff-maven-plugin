package com.github.aezo.maven.plugin.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
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
        
        @SuppressWarnings("unchecked")
        Map<String, List<VersionConflict>> conflictMap = (Map<String, List<VersionConflict>>) 
                parseConflictsMethod.invoke(strategy, output);

        // Then - should find two conflicts
        assertThat(conflictMap).hasSize(2);
        
        // Verify spring-jcl conflict
        assertThat(conflictMap).containsKey("org.springframework:spring-jcl");
        List<VersionConflict> springConflicts = conflictMap.get("org.springframework:spring-jcl");
        assertThat(springConflicts).hasSize(1);
        VersionConflict springConflict = springConflicts.get(0);
        assertThat(springConflict.getPomVersion()).isEqualTo(new ComparableVersion("5.3.21"));
        assertThat(springConflict.getResolvedVersion()).isEqualTo(new ComparableVersion("5.3.20"));
        assertThat(springConflict.getCount()).isEqualTo(1);
        
        // Verify junit conflict
        assertThat(conflictMap).containsKey("junit:junit");
        List<VersionConflict> junitConflicts = conflictMap.get("junit:junit");
        assertThat(junitConflicts).hasSize(1);
        VersionConflict junitConflict = junitConflicts.get(0);
        assertThat(junitConflict.getPomVersion()).isEqualTo(new ComparableVersion("4.13.1"));
        assertThat(junitConflict.getResolvedVersion()).isEqualTo(new ComparableVersion("4.13.2"));
        assertThat(junitConflict.getCount()).isEqualTo(1);
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

    @Test
    void testConvertToConflictObjects() throws Exception {
        // Given - conflict map with version conflicts
        Map<String, List<VersionConflict>> conflictMap = new HashMap<>();
        conflictMap.put("org.springframework:spring-jcl", Arrays.asList(
            new VersionConflict(new ComparableVersion("5.3.21"), new ComparableVersion("5.3.20"), 1)
        ));
        conflictMap.put("junit:junit", Arrays.asList(
            new VersionConflict(new ComparableVersion("4.13.1"), new ComparableVersion("4.13.2"), 1),
            new VersionConflict(new ComparableVersion("4.13.1"), new ComparableVersion("4.13.2"), 1) // Duplicate for merging test
        ));

        // When - convert to conflict objects using reflection
        Method convertMethod = MavenTreeConflictDetectionStrategy.class
                .getDeclaredMethod("convertToConflictObjects", Map.class);
        convertMethod.setAccessible(true);
        
        @SuppressWarnings("unchecked")
        List<DependencyConflict> conflicts = (List<DependencyConflict>) convertMethod.invoke(strategy, conflictMap);

        // Then - should have two dependency conflicts
        assertThat(conflicts).hasSize(2);
        
        // Find spring conflict
        DependencyConflict springConflict = conflicts.stream()
                .filter(c -> c.getArtifactKey().equals("org.springframework:spring-jcl"))
                .findFirst()
                .orElse(null);
        assertThat(springConflict).isNotNull();
        assertThat(springConflict.getConflicts()).hasSize(1);
        
        // Find junit conflict - should have merged the duplicate conflicts
        DependencyConflict junitConflict = conflicts.stream()
                .filter(c -> c.getArtifactKey().equals("junit:junit"))
                .findFirst()
                .orElse(null);
        assertThat(junitConflict).isNotNull();
        assertThat(junitConflict.getConflicts()).hasSize(1);
        VersionConflict mergedConflict = junitConflict.getConflicts().iterator().next();
        assertThat(mergedConflict.getCount()).isEqualTo(2); // Should be merged
    }

    @Test
    void testCollectTransitiveDependencyConflicts_successfulExecution() throws Exception {
        // Given - mock project setup
        File mockBasedir = mock(File.class);
        when(mockBasedir.getAbsolutePath()).thenReturn("/test/project");
        when(mockProject.getBasedir()).thenReturn(mockBasedir);
        when(mockProject.getArtifactId()).thenReturn("test-artifact");
        when(mockProject.getModules()).thenReturn(new ArrayList<>());

        // Mock process execution with sample output containing conflicts
        String sampleOutput = "[INFO] com.example:test-project:jar:1.0.0\n" +
                "[INFO] +- org.springframework:spring-core:jar:5.3.21:compile\n" +
                "[INFO] |  \\- (org.springframework:spring-jcl:jar:5.3.21:compile - omitted for conflict with 5.3.20)\n";
        
        try (MockedStatic<Runtime> runtimeMock = mockStatic(Runtime.class)) {
            Runtime mockRuntime = mock(Runtime.class);
            Process mockProcess = mock(Process.class);
            
            runtimeMock.when(Runtime::getRuntime).thenReturn(mockRuntime);
            when(mockRuntime.exec(any(String[].class), any(), any(File.class))).thenReturn(mockProcess);
            when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream(sampleOutput.getBytes()));
            when(mockProcess.waitFor()).thenReturn(0);

            // When - collect conflicts
            List<DependencyConflict> conflicts = strategy.collectTransitiveDependencyConflicts("test-branch");

            // Then - should find conflicts
            assertThat(conflicts).hasSize(1);
            assertThat(conflicts.get(0).getArtifactKey()).isEqualTo("org.springframework:spring-jcl");
        }
    }

    @Test
    void testCollectTransitiveDependencyConflicts_commandFailure() throws Exception {
        // Given - mock project setup
        File mockBasedir = mock(File.class);
        when(mockBasedir.getAbsolutePath()).thenReturn("/test/project");
        when(mockProject.getBasedir()).thenReturn(mockBasedir);
        when(mockProject.getArtifactId()).thenReturn("test-artifact");
        when(mockProject.getModules()).thenReturn(new ArrayList<>());

        try (MockedStatic<Runtime> runtimeMock = mockStatic(Runtime.class)) {
            Runtime mockRuntime = mock(Runtime.class);
            Process mockProcess = mock(Process.class);
            
            runtimeMock.when(Runtime::getRuntime).thenReturn(mockRuntime);
            when(mockRuntime.exec(any(String[].class), any(), any(File.class))).thenReturn(mockProcess);
            when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream("".getBytes()));
            when(mockProcess.waitFor()).thenReturn(1); // Non-zero exit code

            // When/Then - should throw exception
            assertThatThrownBy(() -> strategy.collectTransitiveDependencyConflicts("test-branch"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Maven dependency:tree command failed with exit code: 1");
        }
    }

    @Test
    void testCollectTransitiveDependencyConflicts_withMultiModuleProject() throws Exception {
        // Given - mock multi-module project setup
        File mockBasedir = mock(File.class);
        when(mockBasedir.getAbsolutePath()).thenReturn("/test/project");
        when(mockProject.getBasedir()).thenReturn(mockBasedir);
        when(mockProject.getArtifactId()).thenReturn("test-artifact");
        when(mockProject.getModules()).thenReturn(Arrays.asList("module1", "module2"));

        String sampleOutput = "[INFO] No conflicts found\n";
        
        try (MockedStatic<Runtime> runtimeMock = mockStatic(Runtime.class)) {
            Runtime mockRuntime = mock(Runtime.class);
            Process mockProcess = mock(Process.class);
            
            runtimeMock.when(Runtime::getRuntime).thenReturn(mockRuntime);
            when(mockRuntime.exec(any(String[].class), any(), any(File.class))).thenReturn(mockProcess);
            when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream(sampleOutput.getBytes()));
            when(mockProcess.waitFor()).thenReturn(0);

            // When - collect conflicts
            List<DependencyConflict> conflicts = strategy.collectTransitiveDependencyConflicts("test-branch");

            // Then - should handle multi-module case and find no conflicts
            assertThat(conflicts).isEmpty();
        }
    }
}
