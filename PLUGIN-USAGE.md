# Maven Conflict Diff Plugin Usage

## Overview

The Maven Conflict Diff Plugin analyzes and compares dependency conflicts between Git branches directly within your Maven build process. It seamlessly integrates with Maven builds and CI/CD pipelines, providing automated dependency conflict analysis as part of your development workflow.

## Installation

### Option 1: Install to Local Repository

```bash
mvn clean install
```

### Option 2: Build and Use Directly

```bash
mvn clean compile
```

## Usage

### Basic Usage

Run the plugin in your Maven project:

```bash
mvn conflict-diff:analyze
```

This will compare dependency conflicts between the current branch and the default base branch (`develop`).

### Specify a Different Base Branch

```bash
mvn conflict-diff:analyze -Dconflict-diff.baseBranch=master
```

### Enable Debug Output

```bash
mvn conflict-diff:analyze -Dconflict-diff.debug=true
```

### Skip Plugin Execution

```bash
mvn conflict-diff:analyze -Dconflict-diff.skip=true
```

## Configuration in pom.xml

You can configure the plugin in your project's `pom.xml`:

```xml
<project>
  <!-- ... -->
  <build>
    <plugins>
      <plugin>
        <groupId>com.github.aezo</groupId>
        <artifactId>conflict-diff-maven-plugin</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <configuration>
          <baseBranch>master</baseBranch>
          <debug>false</debug>
          <skip>false</skip>
        </configuration>
        <executions>
          <execution>
            <goals>
              <goal>analyze</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```

## Integration with Maven Lifecycle

You can bind the plugin to a specific Maven lifecycle phase:

```xml
<plugin>
  <groupId>com.github.aezo</groupId>
  <artifactId>conflict-diff-maven-plugin</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <executions>
    <execution>
      <phase>verify</phase>
      <goals>
        <goal>analyze</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

## Parameters

| Parameter | Property | Default | Description |
|-----------|----------|---------|-------------|
| `baseBranch` | `conflict-diff.baseBranch` | `develop` | The base branch to compare against |
| `debug` | `conflict-diff.debug` | `false` | Enable debug output |
| `skip` | `conflict-diff.skip` | `false` | Skip plugin execution |

## Requirements

- Java 8 or higher
- Maven 3.6.3 or higher
- Git repository
- Maven project with `pom.xml`

## Output

The plugin provides clear and actionable output:

- ✅ Success message if no dependency conflict differences are found
- ⚠️ Detailed list of added and removed conflicts if differences are detected
- ❌ Comprehensive error messages for any failures during execution

### Sample Output

When differences are found, the output includes detailed information about each conflict:

```
[INFO] Analyzing dependency conflicts between 'develop' and 'feature-branch'
[INFO] ⚠️ Differences in dependency conflicts found between branches:
[INFO] Removed conflicts:
[INFO]   - 2 com.example:library-a:jar:1.0.0:compile conflict with 2.0.0
[INFO] Added conflicts:
[INFO]   + 3 com.example:library-b:jar:1.5.0:compile conflict with 1.4.0
```

## Examples

### Compare against master branch with debug output
```bash
mvn conflict-diff:analyze -Dconflict-diff.baseBranch=master -Dconflict-diff.debug=true
```

### Run as part of the verify phase
```bash
mvn verify
```
(If configured in pom.xml to run during verify phase)

### Skip during CI builds
```bash
mvn install -Dconflict-diff.skip=true
```

## CI/CD Integration

### GitHub Actions

```yaml
- name: Analyze Dependency Conflicts
  run: mvn conflict-diff:analyze -Dconflict-diff.baseBranch=main
```

### Jenkins Pipeline

```groovy
stage('Dependency Conflict Analysis') {
    steps {
        sh 'mvn conflict-diff:analyze -Dconflict-diff.baseBranch=main'
    }
}
```

### GitLab CI

```yaml
dependency-analysis:
  script:
    - mvn conflict-diff:analyze -Dconflict-diff.baseBranch=main
```

## Troubleshooting

1. **Plugin not found**: Make sure you've installed the plugin to your local repository with `mvn install`
2. **Git errors**: Ensure you're running the plugin from within a Git repository
3. **Maven errors**: Verify that `mvn dependency:tree` works in your project
4. **Permission errors**: Ensure the plugin has permission to execute Git commands and Maven commands
