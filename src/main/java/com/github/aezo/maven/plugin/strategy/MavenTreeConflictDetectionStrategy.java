package com.github.aezo.maven.plugin.strategy;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.project.MavenProject;

import com.github.aezo.maven.plugin.model.DependencyConflict;
import com.github.aezo.maven.plugin.model.DependencyConflictImpl;
import com.github.aezo.maven.plugin.model.VersionConflict;

/**
 * Conflict detection strategy that uses Maven's dependency:tree command with
 * verbose output
 * to detect dependency conflicts by parsing the command line output.
 * 
 * This strategy executes "mvn dependency:tree -Dverbose -pl <module>" and
 * parses
 * the output to identify conflicts indicated by "omitted for conflict with"
 * messages.
 */
public class MavenTreeConflictDetectionStrategy implements ConflictDetectionStrategy {

    private final MavenProject project;
    private final Consumer<String> debugLogger;

    // Pattern to match conflict lines in Maven dependency tree output
    // Example: [INFO] | | \- (org.springframework:spring-jcl:jar:5.3.21:compile -
    // omitted for conflict with 5.3.20)
    private static final Pattern CONFLICT_PATTERN = Pattern.compile(
            ".*\\(([^:]+):([^:]+):([^:]+):([^:]+):([^)]+)\\s*-\\s*omitted for conflict with\\s+([^)]+)\\)");

    public MavenTreeConflictDetectionStrategy(MavenProject project, Consumer<String> debugLogger) {
        this.project = project;
        this.debugLogger = debugLogger;
    }

    @Override
    public List<DependencyConflict> collectTransitiveDependencyConflicts(String branchName) {
        try {
            debugLogger.accept(
                    "⏳ Collecting transitive dependency conflicts using mvn dependency:tree for branch: " + branchName);

            // Execute Maven dependency:tree command
            List<String> output = executeMavenDependencyTree();

            // Parse the output to extract conflicts
            Map<String, DependencyConflict> conflictMap = parseConflicts(output);

            // Convert to DependencyConflict objects
            List<DependencyConflict> dependencyConflicts = conflictMap.values().stream().collect(Collectors.toList());

            debugLogger
                    .accept("Found " + dependencyConflicts.size() + " dependency conflicts for branch: " + branchName);

            return dependencyConflicts;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to collect transitive dependency conflicts using mvn dependency:tree for branch: "
                            + branchName,
                    e);
        }
    }

    /**
     * Executes the Maven dependency:tree command with verbose output.
     * 
     * @return List of output lines from the command
     * @throws IOException          if command execution fails
     * @throws InterruptedException if command is interrupted
     */
    private List<String> executeMavenDependencyTree() throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add("mvn");
        command.add("dependency:tree");
        command.add("-Dverbose");

        debugLogger.accept("Modules: " + project.getModules());

        // Add module specification if this is a multi-module project
        if (project.getModules() != null && !project.getModules().isEmpty()) {
            String moduleName = project.getArtifactId();
            command.add("-pl");
            command.add(moduleName);
        }

        debugLogger.accept("Executing command: " + String.join(" ", command));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File(project.getBasedir().getAbsolutePath()));
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        List<String> output = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.add(line);
                if (debugLogger != null) {
                    debugLogger.accept("Maven output: " + line);
                }
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Maven dependency:tree command failed with exit code: " + exitCode);
        }

        return output;
    }

    /**
     * Parses the Maven dependency tree output to extract conflict information.
     * 
     * @param output List of output lines from Maven command
     * @return Map of artifact keys to lists of version conflicts
     */
    private Map<String, DependencyConflict> parseConflicts(List<String> output) {
        Map<String, DependencyConflict> dependencyConflictMap = new HashMap<>();

        for (String line : output) {
            Matcher matcher = CONFLICT_PATTERN.matcher(line);
            if (matcher.find()) {
                String groupId = matcher.group(1);
                String artifactId = matcher.group(2);
                // String packaging = matcher.group(3); // Not used
                String omittedVersion = matcher.group(4);
                // String scope = matcher.group(5); // Not used
                String winningVersion = matcher.group(6);

                String artifactKey = groupId + ":" + artifactId;

                debugLogger.accept(
                        "Found conflict for " + artifactKey + ": " + omittedVersion + " omitted for " + winningVersion);

                // Create version conflict
                ComparableVersion pomVersion = new ComparableVersion(omittedVersion);
                ComparableVersion resolvedVersion = new ComparableVersion(winningVersion);
                VersionConflict versionConflict = new VersionConflict(pomVersion, resolvedVersion, 1);

                // Add to conflict map
                dependencyConflictMap.computeIfAbsent(artifactKey, k -> new DependencyConflictImpl(artifactKey)).addConflict(versionConflict);
            }
        }

        return dependencyConflictMap;
    }
}
