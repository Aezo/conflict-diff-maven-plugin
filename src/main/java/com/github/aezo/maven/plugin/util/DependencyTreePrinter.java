package com.github.aezo.maven.plugin.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.eclipse.aether.graph.DependencyNode;

/**
 * Utility class for printing dependency trees in Maven's standard format.
 * This class provides functionality to display dependency trees similar to
 * the output of Maven's dependency:tree goal, with proper ASCII tree formatting
 * and indentation.
 */
public class DependencyTreePrinter {
    
    private final Log logger;
    
    /**
     * Creates a new DependencyTreePrinter with the specified logger.
     * 
     * @param logger the Maven logger to use for output
     */
    public DependencyTreePrinter(Log logger) {
        this.logger = logger;
    }
    
    /**
     * Prints the entire dependency tree starting from the root node.
     * The output follows Maven's standard dependency:tree format with
     * ASCII tree characters and proper indentation.
     * 
     * @param rootNode the root node of the dependency tree
     */
    public void printDependencyTree(DependencyNode rootNode) {
        if (rootNode == null) {
            logger.info("No dependency tree to display");
            return;
        }
        
        logger.info("Dependency Tree:");
        logger.info("");
        
        // Print the root node first
        printRootNode(rootNode);
        
        // Print all child nodes
        List<DependencyNode> children = rootNode.getChildren();
        if (children != null && !children.isEmpty()) {
            printChildren(children, new ArrayList<>());
        }
    }
    
    /**
     * Prints the root node of the dependency tree.
     * 
     * @param rootNode the root node to print
     */
    private void printRootNode(DependencyNode rootNode) {
        String nodeStr = formatDependencyNode(rootNode);
        logger.info(nodeStr);
    }
    
    /**
     * Recursively prints child nodes with proper tree formatting.
     * 
     * @param children the list of child nodes to print
     * @param isLastAtLevel list indicating if each level is the last child
     */
    private void printChildren(List<DependencyNode> children, List<Boolean> isLastAtLevel) {
        for (int i = 0; i < children.size(); i++) {
            DependencyNode child = children.get(i);
            boolean isLastChild = (i == children.size() - 1);
            
            // Build the prefix for this node
            String nodePrefix = buildNodePrefix(isLastAtLevel, isLastChild);
            
            // Print the current node
            String nodeStr = formatDependencyNode(child);
            logger.info(nodePrefix + nodeStr);
            
            // Recursively print children if they exist
            List<DependencyNode> grandChildren = child.getChildren();
            if (grandChildren != null && !grandChildren.isEmpty()) {
                // Create new level tracking for recursion
                List<Boolean> newIsLastAtLevel = new ArrayList<>(isLastAtLevel);
                newIsLastAtLevel.add(isLastChild);
                
                printChildren(grandChildren, newIsLastAtLevel);
            }
        }
    }
    
    /**
     * Builds the prefix for a node based on its position in the tree.
     * 
     * @param isLastAtLevel list indicating if each level is the last child
     * @param isLastChild whether this node is the last child at its level
     * @return the prefix string for the node
     */
    private String buildNodePrefix(List<Boolean> isLastAtLevel, boolean isLastChild) {
        StringBuilder prefix = new StringBuilder();
        
        // Add vertical bars for parent levels
        for (Boolean isLast : isLastAtLevel) {
            if (isLast) {
                prefix.append("   "); // Three spaces for completed branches
            } else {
                prefix.append("│  "); // Vertical bar with two spaces
            }
        }
        
        // Add the connector for this level
        if (isLastChild) {
            prefix.append("└─ "); // Last child connector
        } else {
            prefix.append("├─ "); // Normal child connector  
        }
        
        return prefix.toString();
    }
    
    /**
     * Formats a dependency node into Maven's standard string representation.
     * Format: groupId:artifactId:type:version:scope
     * 
     * @param node the dependency node to format
     * @return the formatted string representation
     */
    private String formatDependencyNode(DependencyNode node) {
        if (node == null || node.getArtifact() == null) {
            return "unknown";
        }
        
        org.eclipse.aether.artifact.Artifact artifact = node.getArtifact();
        
        StringBuilder sb = new StringBuilder();
        sb.append(artifact.getGroupId())
          .append(":")
          .append(artifact.getArtifactId())
          .append(":")
          .append(artifact.getExtension()) // This is the type/packaging
          .append(":")
          .append(artifact.getVersion());
        
        // Add scope if available from the dependency
        if (node.getDependency() != null && node.getDependency().getScope() != null) {
            sb.append(":").append(node.getDependency().getScope());
        }
        
        return sb.toString();
    }
    
    /**
     * Prints a compact summary of the dependency tree showing only artifact counts.
     * 
     * @param rootNode the root node of the dependency tree
     */
    public void printDependencyTreeSummary(DependencyNode rootNode) {
        if (rootNode == null) {
            logger.info("No dependency tree to summarize");
            return;
        }
        
        int totalNodes = countNodes(rootNode);
        logger.info("Dependency Tree Summary: " + totalNodes + " total dependencies");
    }
    
    /**
     * Counts the total number of nodes in the dependency tree.
     * 
     * @param node the root node to start counting from
     * @return the total number of nodes
     */
    private int countNodes(DependencyNode node) {
        if (node == null) {
            return 0;
        }
        
        int count = 1; // Count this node
        
        List<DependencyNode> children = node.getChildren();
        if (children != null) {
            for (DependencyNode child : children) {
                count += countNodes(child);
            }
        }
        
        return count;
    }
}
