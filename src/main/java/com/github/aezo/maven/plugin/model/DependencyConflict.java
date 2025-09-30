package com.github.aezo.maven.plugin.model;

import java.util.Map;

public interface DependencyConflict {
    String getArtifactKey();
    Map<String, VersionConflict> getConflicts();
    void addConflict(VersionConflict conflict);
    void add(DependencyConflict other);
    void union(DependencyConflict other);
    DependencyConflict diff(DependencyConflict base);
}
