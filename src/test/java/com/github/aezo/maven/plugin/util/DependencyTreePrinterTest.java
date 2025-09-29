package com.github.aezo.maven.plugin.util;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.eclipse.aether.graph.DependencyNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for DependencyTreePrinter.
 */
@ExtendWith(MockitoExtension.class)
class DependencyTreePrinterTest {

    @Mock
    private Log mockLogger;

    private DependencyTreePrinter printer;

    @BeforeEach
    void setUp() {
        printer = new DependencyTreePrinter(mockLogger);
    }

    @Test
    void testPrintDependencyTreeWithNull() {
        // When
        printer.printDependencyTree(null);

        // Then
        verify(mockLogger).info("No dependency tree to display");
    }

    @Test
    void testPrintDependencyTreeWithSingleNode() {
        // Given
        DependencyNode rootNode = createMockRootNode();
        when(rootNode.getChildren()).thenReturn(new ArrayList<>());

        // When
        printer.printDependencyTree(rootNode);

        // Then
        verify(mockLogger).info("Dependency Tree:");
        verify(mockLogger).info("");
        verify(mockLogger).info("com.example:root-artifact:jar:1.0.0");
    }

    @Test
    void testPrintDependencyTreeWithChildren() {
        // Given
        DependencyNode rootNode = createMockRootNode();
        DependencyNode child1 = createMockChildNode("org.example", "child1", "2.0.0");
        DependencyNode child2 = createMockChildNode("org.example", "child2", "3.0.0");

        List<DependencyNode> children = new ArrayList<>();
        children.add(child1);
        children.add(child2);
        
        when(rootNode.getChildren()).thenReturn(children);
        when(child1.getChildren()).thenReturn(new ArrayList<>());
        when(child2.getChildren()).thenReturn(new ArrayList<>());

        // When
        printer.printDependencyTree(rootNode);

        // Then
        verify(mockLogger).info("Dependency Tree:");
        verify(mockLogger).info("");
        verify(mockLogger).info("com.example:root-artifact:jar:1.0.0");
        verify(mockLogger).info("├─ org.example:child1:jar:2.0.0");
        verify(mockLogger).info("└─ org.example:child2:jar:3.0.0");
    }

    @Test
    void testPrintDependencyTreeWithNestedChildren() {
        // Given
        DependencyNode rootNode = createMockRootNode();
        DependencyNode child1 = createMockChildNode("org.example", "child1", "2.0.0");
        DependencyNode grandchild = createMockChildNode("com.nested", "grandchild", "1.5.0");

        List<DependencyNode> children = new ArrayList<>();
        children.add(child1);
        
        List<DependencyNode> grandchildren = new ArrayList<>();
        grandchildren.add(grandchild);

        when(rootNode.getChildren()).thenReturn(children);
        when(child1.getChildren()).thenReturn(grandchildren);
        when(grandchild.getChildren()).thenReturn(new ArrayList<>());

        // When
        printer.printDependencyTree(rootNode);

        // Then
        verify(mockLogger).info("Dependency Tree:");
        verify(mockLogger).info("");
        verify(mockLogger).info("com.example:root-artifact:jar:1.0.0");
        verify(mockLogger).info("└─ org.example:child1:jar:2.0.0");
        verify(mockLogger).info("   └─ com.nested:grandchild:jar:1.5.0");
    }

    @Test
    void testPrintDependencyTreeSummary() {
        // Given
        DependencyNode rootNode = createMockRootNode();
        DependencyNode child1 = createMockChildNode("org.example", "child1", "2.0.0");

        List<DependencyNode> children = new ArrayList<>();
        children.add(child1);

        when(rootNode.getChildren()).thenReturn(children);
        when(child1.getChildren()).thenReturn(new ArrayList<>());

        // When
        printer.printDependencyTreeSummary(rootNode);

        // Then
        verify(mockLogger).info(contains("Dependency Tree Summary: 2 total dependencies"));
    }

    @Test
    void testPrintDependencyTreeSummaryWithNull() {
        // When
        printer.printDependencyTreeSummary(null);

        // Then
        verify(mockLogger).info("No dependency tree to summarize");
    }

    private DependencyNode createMockRootNode() {
        DependencyNode rootNode = mock(DependencyNode.class);
        org.eclipse.aether.artifact.Artifact rootArtifact = mock(org.eclipse.aether.artifact.Artifact.class);
        
        lenient().when(rootArtifact.getGroupId()).thenReturn("com.example");
        lenient().when(rootArtifact.getArtifactId()).thenReturn("root-artifact");
        lenient().when(rootArtifact.getVersion()).thenReturn("1.0.0");
        lenient().when(rootArtifact.getExtension()).thenReturn("jar");
        
        lenient().when(rootNode.getArtifact()).thenReturn(rootArtifact);
        
        return rootNode;
    }

    private DependencyNode createMockChildNode(String groupId, String artifactId, String version) {
        DependencyNode childNode = mock(DependencyNode.class);
        org.eclipse.aether.artifact.Artifact childArtifact = mock(org.eclipse.aether.artifact.Artifact.class);
        
        lenient().when(childArtifact.getGroupId()).thenReturn(groupId);
        lenient().when(childArtifact.getArtifactId()).thenReturn(artifactId);
        lenient().when(childArtifact.getVersion()).thenReturn(version);
        lenient().when(childArtifact.getExtension()).thenReturn("jar");
        
        lenient().when(childNode.getArtifact()).thenReturn(childArtifact);
        
        return childNode;
    }
}
