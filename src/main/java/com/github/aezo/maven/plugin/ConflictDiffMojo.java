package com.github.aezo.maven.plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;

import de.vandermeer.asciitable.AsciiTable;

import com.github.aezo.maven.plugin.model.DependencyConflict;
import com.github.aezo.maven.plugin.model.VersionConflict;
import com.github.aezo.maven.plugin.strategy.ConflictDetectionStrategy;
import com.github.aezo.maven.plugin.strategy.DefaultConflictDetectionStrategy;
import com.github.aezo.maven.plugin.util.GitRepositoryUtil;

/**
 * Maven plugin goal that compares transitive dependency conflicts between Git
 * branches.
 * 
 * This plugin analyzes transitive dependency conflicts by building dependency
 * graphs
 * for both a base branch and the current branch using Maven's internal APIs.
 * It identifies cases where transitive dependencies are overridden due to
 * version
 * conflicts and compares these conflicts between branches to detect what has
 * changed.
 * 
 * A transitive dependency conflict occurs when multiple versions of the same
 * artifact
 * exist in the dependency tree and Maven's nearest-first resolution strategy
 * chooses
 * one version over another, causing some transitive dependencies to be omitted.
 */
@Mojo(name = "analyze", requiresProject = true, requiresDependencyResolution = ResolutionScope.TEST)
public class ConflictDiffMojo extends AbstractMojo {

    /**
     * The base branch to compare against.
     */
    @Parameter(property = "conflict-diff.baseBranch", defaultValue = "develop")
    private String baseBranch;

    /**
     * Enable debug output.
     */
    @Parameter(property = "conflict-diff.debug", defaultValue = "false")
    private boolean debug;

    /**
     * The Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * Skip the plugin execution.
     */
    @Parameter(property = "conflict-diff.skip", defaultValue = "false")
    private boolean skip;

    /**
     * The repository system session.
     */
    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
    private RepositorySystemSession repoSession;

    /**
     * The project's remote repositories to use for the resolution of project
     * dependencies.
     */
    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    private List<RemoteRepository> remoteRepos;

    /**
     * The repository system.
     */
    @Component
    private RepositorySystem repositorySystem;

    private ConflictDetectionStrategy conflictDetectionStrategy;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("⏩ Skipping dependency conflict analysis");
            return;
        }

        try {
            Repository repository = GitRepositoryUtil.getRepository(project.getBasedir());
            Git git = new Git(repository);

            String currentBranch = GitRepositoryUtil.getCurrentBranch(git);

            if (currentBranch.equals(baseBranch)) {
                getLog().info("⏩ Current branch (" + currentBranch + ") is the same as base branch (" + baseBranch
                        + "). Skipping analysis.");
                return;
            }

            getLog().info("⏳ Analyzing dependency conflicts between '" + baseBranch + "' and '" + currentBranch + "'");

            // Initialize the conflict detection strategy
            conflictDetectionStrategy = new DefaultConflictDetectionStrategy(
                    project, repositorySystem, repoSession, remoteRepos, this::debugLog);

            // Collect transitive dependency conflicts from base branch
            debugLog("⏳ Checking out base branch: " + baseBranch);
            GitRepositoryUtil.checkoutBranch(git, baseBranch);
            List<DependencyConflict> baseConflicts = conflictDetectionStrategy.collectTransitiveDependencyConflicts(baseBranch);

            // Collect transitive dependency conflicts from current branch
            debugLog("⏳ Checking out current branch: " + currentBranch);
            GitRepositoryUtil.checkoutBranch(git, currentBranch);
            List<DependencyConflict> currentConflicts = conflictDetectionStrategy.collectTransitiveDependencyConflicts(currentBranch);

            // Compare and report differences
            debugLog("⏳ Comparing and reporting differences");
            Map<String, List<DependencyConflict>> conflicts = compareTransitiveDependencyConflicts(baseConflicts, currentConflicts);
            reportConflicts(conflicts);

        } catch (Exception e) {
            throw new MojoExecutionException("❌ Failed to analyze dependency conflicts", e);
        }
    }


    private Map<String, List<DependencyConflict>> compareTransitiveDependencyConflicts(List<DependencyConflict> baseConflicts, List<DependencyConflict> currentConflicts) {
        debugLog("⏳ Comparing transitive dependency conflicts");

        // Convert lists to maps for easy lookup by artifact key
        Map<String, DependencyConflict> baseConflictsMap = baseConflicts.stream()
            .collect(Collectors.toMap(DependencyConflict::getArtifactKey, Function.identity()));
        Map<String, DependencyConflict> currentConflictsMap = currentConflicts.stream()
            .collect(Collectors.toMap(DependencyConflict::getArtifactKey, Function.identity()));


        // Lists to collect different types of conflicts
        List<DependencyConflict> resolvedConflicts = new ArrayList<>();
        List<DependencyConflict> newConflicts = new ArrayList<>();
        List<DependencyConflict> existingConflictsDiffs = new ArrayList<>();

        // Process conflicts from base branch
        for (Map.Entry<String, DependencyConflict> entry : baseConflictsMap.entrySet()) {
            String conflictKey = entry.getKey();
            DependencyConflict baseConflict = entry.getValue();
            DependencyConflict currentConflict = currentConflictsMap.get(conflictKey);
            
            if (currentConflict == null) {
                // Conflict was resolved (exists in base but not in current)
                resolvedConflicts.add(baseConflict);
            } else {
                // Conflict exists in both branches - compute diff
                DependencyConflict diff = currentConflict.diff(baseConflict);
                // Only include if there are actual differences (non-zero counts)
                if (hasNonZeroConflicts(diff)) {
                    existingConflictsDiffs.add(diff);
                }
            }
        }

        // Process conflicts from current branch to find new ones
        for (Map.Entry<String, DependencyConflict> entry : currentConflictsMap.entrySet()) {
            String conflictKey = entry.getKey();
            DependencyConflict currentConflict = entry.getValue();
            
            if (!baseConflictsMap.containsKey(conflictKey)) {
                // New conflict (exists in current but not in base)
                newConflicts.add(currentConflict);
            }
        }

        // return map of resolved conflicts, new conflicts, and existing conflicts with differences
        // use "resolved", "new", and "existing" as keys
        Map<String, List<DependencyConflict>> result = new HashMap<>();
        result.put("resolved", resolvedConflicts);
        result.put("new", newConflicts);
        result.put("diff", existingConflictsDiffs);
        return result;
    }

    private void reportConflicts(Map<String, List<DependencyConflict>> conflicts) {
        List<DependencyConflict> resolvedConflicts = conflicts.get("resolved");
        List<DependencyConflict> newConflicts = conflicts.get("new");
        List<DependencyConflict> existingConflictsDiffs = conflicts.get("diff");

        if (resolvedConflicts.isEmpty() && newConflicts.isEmpty() && existingConflictsDiffs.isEmpty()) {
            getLog().info("🎉 No new conflicts found in feature branch!");
            return;
        }

        getLog().info("⚠️  Transitive dependency conflict differences found between branches:");
        getLog().info("");

        // Report resolved conflicts
        if (!resolvedConflicts.isEmpty()) {
            getLog().info("✅ RESOLVED CONFLICTS (present in base branch but not in current branch):");
            printConflictTable(resolvedConflicts, "RESOLVED");
            printConflictSummary(resolvedConflicts);
            getLog().info("");
        }

        // Report new conflicts
        if (!newConflicts.isEmpty()) {
            getLog().info("❌ NEW CONFLICTS (present in current branch but not in base branch):");
            printConflictTable(newConflicts, "NEW");
            printConflictSummary(newConflicts);
            getLog().info("");
        }

        // Report existing conflicts with differences
        if (!existingConflictsDiffs.isEmpty()) {
            getLog().info("📊 CHANGED CONFLICTS (diff between branches):");
            printConflictTable(existingConflictsDiffs, "CHANGED");
            printConflictSummary(existingConflictsDiffs);
            getLog().info("");
        }

        // Summary
        getLog().info("📋 SUMMARY: " + resolvedConflicts.size() + " resolved, " + newConflicts.size() + " new, " + existingConflictsDiffs.size() + " changed");
    }

    private void printConflictTable(List<DependencyConflict> conflicts, String conflictType) {
        if (conflicts.isEmpty()) {
            return;
        }

        AsciiTable table = new AsciiTable();
        table.addRule();
        table.addRow("ARTIFACT", "VERSION CONFLICT", "TYPE", "COUNT");
        table.addRule();

        for (DependencyConflict conflict : conflicts) {
            boolean firstRow = true;
            for (VersionConflict vc : conflict.getConflicts()) {
                String artifactKey = firstRow ? conflict.getArtifactKey() : "";
                String versionConflict = vc.getPomVersion() + " → " + vc.getResolvedVersion();
                String conflictTypeIndicator = getConflictTypeIndicator(vc);
                String count = formatCount(vc.getCount(), conflictType);
                
                table.addRow(artifactKey, versionConflict, conflictTypeIndicator, count);
                firstRow = false;
            }
            
            // Add separator between different artifacts for better readability
            if (conflict.getConflicts().size() > 1) {
                table.addRule();
            }
        }
        
        table.addRule();
        
        // Print each line of the table
        String tableString = table.render();
        for (String line : tableString.split("\n")) {
            getLog().info(line);
        }
    }

    /**
     * Prints a summary of conflict types (upgrades vs downgrades) for a list of conflicts.
     * 
     * @param conflicts the list of dependency conflicts to summarize
     */
    private void printConflictSummary(List<DependencyConflict> conflicts) {
        int upgradeCount = 0;
        int downgradeCount = 0;
        int equalCount = 0;
        
        for (DependencyConflict conflict : conflicts) {
            for (VersionConflict vc : conflict.getConflicts()) {
                int comparison = vc.getPomVersion().compareTo(vc.getResolvedVersion());
                if (comparison > 0) {
                    downgradeCount++;
                } else if (comparison < 0) {
                    upgradeCount++;
                } else {
                    equalCount++;
                }
            }
        }
        
        StringBuilder summary = new StringBuilder("   ");
        if (upgradeCount > 0) {
            summary.append("🔺 ").append(upgradeCount).append(" upgrades");
        }
        if (downgradeCount > 0) {
            if (upgradeCount > 0) summary.append(", ");
            summary.append("🔻 ").append(downgradeCount).append(" downgrades");
        }
        if (equalCount > 0) {
            if (upgradeCount > 0 || downgradeCount > 0) summary.append(", ");
            summary.append("🟦 ").append(equalCount).append(" equal");
        }
        
        if (summary.length() > 3) { // More than just the initial "   "
            getLog().info(summary.toString());
            
            // Add context about impact
            if (downgradeCount > 0) {
                getLog().info("   ⚠️  Downgrades may indicate missing features or potential compatibility issues");
            }
            if (upgradeCount > 0) {
                getLog().info("   ✨ Upgrades generally provide bug fixes and new features");
            }
        }
    }

    /**
     * Determines the type of version conflict (upgrade/downgrade) and returns a visual indicator.
     * 
     * @param versionConflict the version conflict to analyze
     * @return a string indicating whether this is an upgrade or downgrade
     */
    private String getConflictTypeIndicator(VersionConflict versionConflict) {
        int comparison = versionConflict.getPomVersion().compareTo(versionConflict.getResolvedVersion());
        
        if (comparison > 0) {
            // POM version > resolved version = downgrade (newer version overridden by older)
            return "🔻 DOWNGRADE";
        } else if (comparison < 0) {
            // POM version < resolved version = upgrade (older version overridden by newer)
            return "🔺 UPGRADE";
        } else {
            // Versions are equal (shouldn't happen in conflicts, but just in case)
            return "🟦 EQUAL";
        }
    }

    private String formatCount(int count, String conflictType) {
        if ("CHANGED".equals(conflictType)) {
            return count > 0 ? ("+" + count) : String.valueOf(count);
        } else {
            return String.valueOf(count);
        }
    }

    /**
     * Checks if a DependencyConflict has any version conflicts with non-zero counts.
     * This is used to filter out diffs where there are no actual changes.
     * 
     * @param conflict the DependencyConflict to check
     * @return true if there are conflicts with non-zero counts, false otherwise
     */
    private boolean hasNonZeroConflicts(DependencyConflict conflict) {
        if (conflict == null || conflict.getConflicts() == null || conflict.getConflicts().isEmpty()) {
            return false;
        }
        
        return conflict.getConflicts().stream()
            .anyMatch(vc -> vc.getCount() != 0);
    }

    private void debugLog(String message) {
        if (debug) {
            // Only use color if output is going to a terminal
            if (isTerminal()) {
                String greyMessage = "\033[90m" + message + "\033[0m";
                getLog().info(greyMessage);
            } else {
                getLog().info(message);
            }
        }
    }

    /**
     * Checks if the output is going to a terminal (TTY).
     * Returns false if output is being redirected to a file or piped.
     */
    private boolean isTerminal() {
        // System.console() returns null if not connected to a terminal
        return System.console() != null;
    }
}

