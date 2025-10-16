# Maven Dependency Conflict Analyzer Plugin

A Maven plugin that compares dependency conflicts between Git branches to help identify dependency changes introduced by feature branches.

## Table of Contents

- [Description](#description)
- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Usage](#usage)
- [How It Works](#how-it-works)
- [Output](#output)
- [Alternative Tools](#alternative-tools)
- [Integration Examples](#integration-examples)
- [Contributing](#contributing)
- [License](#license)

## Description

This Maven plugin helps developers understand how their feature branch changes affect Maven dependency resolution by comparing dependency conflicts between the current branch and a base branch (typically `develop` or `master`). It provides detailed dependency conflict analysis that can be run on-demand or configured to run as part of your build process.

## Features

- 🔌 **Maven Integration**: Native Maven plugin that can be invoked directly
- 🔍 **Conflict Detection**: Identifies Maven dependency conflicts using internal Maven dependency resolution APIs
- 🌿 **Branch Comparison**: Compares dependency conflicts between any two Git branches
- 📊 **Detailed Analysis**: Shows count and details of each conflict type
- ⚙️ **Configurable**: Flexible configuration through Maven properties or pom.xml
- 🐛 **Debug Mode**: Optional verbose output for troubleshooting
- 🚀 **CI/CD Ready**: Easy integration with continuous integration pipelines

## Requirements

- **Java 8+**: Required for running the Maven plugin
- **Maven 3.6.3+**: Required for Maven plugin functionality
- **Git repository**: Project must be a Git repository for branch comparison
- **Maven project**: Must have a valid `pom.xml` file with dependencies

## Installation

### Install Plugin to Local Repository

```bash
git clone https://github.com/aezo/conflict-diff-maven-plugin.git
cd conflict-diff-maven-plugin
mvn clean install
```

### Add Plugin to Your Project

Add the plugin to your project's `pom.xml`:

```xml
<build>
  <plugins>
    <plugin>
      <groupId>com.github.aezo</groupId>
      <artifactId>conflict-diff-maven-plugin</artifactId>
      <version>1.0.0-SNAPSHOT</version>
    </plugin>
  </plugins>
</build>
```

## Usage

### Quick Start for Local Development

After installing the plugin locally, here's how to run your first analysis:

```bash
# 1. Clone and install the plugin (one-time setup)
git clone https://github.com/aezo/conflict-diff-maven-plugin.git
cd conflict-diff-maven-plugin
mvn clean install

# 2. Navigate to your project
cd /path/to/your/maven/project

# 3. Run the analysis using full coordinates (recommended for local use)
mvn com.github.aezo:conflict-diff-maven-plugin:1.0.0-SNAPSHOT:analyze -Dconflict-diff.baseBranch=main

# 4. (Optional) Configure Maven settings for shorter commands
# Add com.github.aezo to pluginGroups in ~/.m2/settings.xml, then use:
# mvn conflict-diff:analyze -Dconflict-diff.baseBranch=main
```

### Basic Usage

```bash
# Compare current branch against 'develop' (default base branch)
mvn conflict-diff:analyze

# Compare current branch against 'master'
mvn conflict-diff:analyze -Dconflict-diff.baseBranch=master

# Enable debug output
mvn conflict-diff:analyze -Dconflict-diff.debug=true

# Skip plugin execution
mvn conflict-diff:analyze -Dconflict-diff.skip=true
```

### Configuration in pom.xml

```xml
<plugin>
  <groupId>com.github.aezo</groupId>
  <artifactId>conflict-diff-maven-plugin</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <configuration>
    <baseBranch>master</baseBranch>
    <debug>false</debug>
    <skip>false</skip>
  </configuration>
  <!-- Optional: Bind to Maven lifecycle -->
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

### Multi-Module Projects

For multi-module Maven projects, you can target specific modules:

```bash
# Run on a specific module only
mvn conflict-diff:analyze -pl your-module-name

# Run on multiple specific modules
mvn conflict-diff:analyze -pl module1,module2

# Run on all modules except one
mvn conflict-diff:analyze -pl !module-to-skip

# Combine with other parameters
mvn conflict-diff:analyze -pl your-module-name -Dconflict-diff.baseBranch=master
```

Alternatively, you can navigate to the specific module directory:

```bash
# Navigate to the module and run from there
cd your-specific-module/
mvn conflict-diff:analyze
```

### Parameters

| Parameter    | Property                   | Default   | Description                        |
|--------------|----------------------------|-----------|------------------------------------|
| `baseBranch` | `conflict-diff.baseBranch` | `develop` | The base branch to compare against |
| `debug`      | `conflict-diff.debug`      | `false`   | Enable debug output                |
| `skip`       | `conflict-diff.skip`       | `false`   | Skip plugin execution              |

## Output

The plugin provides clear, actionable output:
- ✅ **Success**: "No differences in dependency conflicts found" when branches have identical conflicts
- ⚠️ **Differences Found**: Detailed list of added and removed conflicts between branches
- ❌ **Errors**: Clear error messages for Git, Maven, or configuration issues

### Sample Output

#### When No Conflicts Found
```
[INFO] ⏳ Analyzing dependency conflicts between 'develop' and 'feature-branch'
[INFO] 🎉 No new conflicts found in feature branch!
```

#### When Conflicts Are Found
```
[INFO] ⏳ Analyzing dependency conflicts between 'develop' and 'feature-branch'
[INFO] ⚠️  Transitive dependency conflict differences found between branches:
[INFO] 
[INFO] ✅ RESOLVED CONFLICTS (present in base branch but not in current branch):
[INFO] ┌─────────────────────────────────────┬──────────────────────┬──────────────┬───────┐
[INFO] │ARTIFACT                             │VERSION CONFLICT      │TYPE          │COUNT  │
[INFO] ├─────────────────────────────────────┼──────────────────────┼──────────────┼───────┤
[INFO] │com.example:library-a:jar:compile    │1.0.0 → 2.0.0         │🔺 UPGRADE    │3      │
[INFO] │com.google:guava:jar:compile         │20.0 → 30.1-jre       │🔺 UPGRADE    │1      │
[INFO] └─────────────────────────────────────┴──────────────────────┴──────────────┴───────┘
[INFO]    🔺 2 upgrades
[INFO]    ✨ Upgrades generally provide bug fixes and new features
[INFO] 
[INFO] ❌ NEW CONFLICTS (present in current branch but not in base branch):
[INFO] ┌─────────────────────────────────────┬──────────────────────┬──────────────┬───────┐
[INFO] │ARTIFACT                             │VERSION CONFLICT      │TYPE          │COUNT  │
[INFO] ├─────────────────────────────────────┼──────────────────────┼──────────────┼───────┤
[INFO] │com.example:library-b:jar:compile    │1.5.0 → 1.4.0         │🔻 DOWNGRADE  │2      │
[INFO] │org.springframework:spring-core:jar  │5.3.0 → 5.2.8.RELEASE │🔻 DOWNGRADE  │1      │
[INFO] │junit:junit:jar:test                 │4.13 → 4.12           │🔻 DOWNGRADE  │1      │
[INFO] └─────────────────────────────────────┴──────────────────────┴──────────────┴───────┘
[INFO]    🔻 3 downgrades
[INFO]    ⚠️  Downgrades may indicate missing features or potential compatibility issues
[INFO] 
[INFO] 📋 SUMMARY: 2 resolved, 3 new, 0 changed
```

This output shows that 2 dependency conflicts were resolved (meaning dependencies that had conflicts in the base branch no longer have conflicts in the current branch), while 3 new conflicts were introduced in the feature branch. The plugin categorizes version changes as upgrades (🔺) or downgrades (🔻) and provides contextual information about their potential impact.

## Alternative Tools

### Bash Script (mvn-conflict-diff)

For users who prefer a standalone command-line tool or cannot use the Maven plugin, this repository also includes a bash script (`scripts/mvn-conflict-diff`) that provides similar functionality.

#### Requirements
- Git repository
- Maven project with `pom.xml`
- `mvn` command available in PATH
- Bash shell (macOS/Linux)

#### Installation
```bash
# Make the script executable
chmod +x scripts/mvn-conflict-diff

# (Optional) Add to your PATH for global access
export PATH="$PATH:/path/to/conflict-diff-maven-plugin/scripts"
```

#### Usage
```bash
# Compare current branch against 'develop' (default)
mvn-conflict-diff

# Compare current branch against 'master'
mvn-conflict-diff master

# Compare against 'master' with debug output
mvn-conflict-diff master --debug

# Show help
mvn-conflict-diff --help
```

#### Migration from Bash Script to Maven Plugin

| Bash Script                          | Maven Plugin                                                                             |
|--------------------------------------|------------------------------------------------------------------------------------------|
| `./mvn-conflict-diff`                | `mvn conflict-diff:analyze`                                                              |
| `./mvn-conflict-diff master`         | `mvn conflict-diff:analyze -Dconflict-diff.baseBranch=master`                            |
| `./mvn-conflict-diff master --debug` | `mvn conflict-diff:analyze -Dconflict-diff.baseBranch=master -Dconflict-diff.debug=true` |

## Integration Examples

### CI/CD Pipeline Integration

#### GitHub Actions
```yaml
- name: Analyze Dependency Conflicts
  run: mvn conflict-diff:analyze -Dconflict-diff.baseBranch=main
```

#### Jenkins Pipeline
```groovy
stage('Dependency Conflict Analysis') {
    steps {
        sh 'mvn conflict-diff:analyze -Dconflict-diff.baseBranch=main'
    }
}
```

#### Optional Maven Lifecycle Binding
```xml
<!-- Optional: Bind to Maven lifecycle phase -->
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

## Contributing

We welcome contributions! Please follow these steps:

1. **Fork the repository** on GitHub
2. **Create your feature branch** (`git checkout -b feature/amazing-feature`)
3. **Make your changes** and add tests if applicable
4. **Commit your changes** (`git commit -m 'Add some amazing feature'`)
5. **Push to the branch** (`git push origin feature/amazing-feature`)
6. **Open a Pull Request** with a clear description of your changes

### Development Setup

```bash
# Clone your fork
git clone https://github.com/your-username/conflict-diff-maven-plugin.git
cd conflict-diff-maven-plugin

# Build and test
mvn clean compile test

# Install to local repository for testing
mvn clean install
```

### Running Tests

```bash
# Run unit tests
mvn test

# Run integration tests (if available)
mvn verify
```

## Troubleshooting

### Common Issues

#### Plugin Not Found
```
[ERROR] Could not find goal 'analyze' in plugin com.github.aezo:conflict-diff-maven-plugin
```
**Solution**: Install the plugin to your local repository first:
```bash
mvn clean install
```

#### Plugin Prefix Not Recognized
```
[ERROR] No plugin found for prefix 'conflict-diff' in the current project
```
This occurs when Maven cannot resolve the `conflict-diff` prefix. You have two solutions:

**Solution 1**: Use the full plugin coordinates:
```bash
mvn com.github.aezo:conflict-diff-maven-plugin:1.0.0-SNAPSHOT:analyze
```

**Solution 2**: Add the plugin group to your Maven settings (`~/.m2/settings.xml`):
```xml
<pluginGroups>
   <pluginGroup>org.sonarsource.scanner.maven</pluginGroup>
   <pluginGroup>com.github.aezo</pluginGroup>  <!-- Add this line -->
</pluginGroups>
```
After updating settings.xml, the short prefix will work:
```bash
mvn conflict-diff:analyze
```

#### Git Repository Error
```
[ERROR] Not a git repository (or any of the parent directories)
```
**Solution**: Ensure you're running the plugin from within a Git repository:
```bash
git init  # If starting a new repository
git status  # Verify you're in a Git repository
```

#### Maven Dependency Tree Error
```
[ERROR] Failed to execute goal on project: dependency:tree failed
```
**Solution**: Ensure your project has a valid `pom.xml` and can build successfully:
```bash
mvn clean compile  # Verify project builds
mvn dependency:tree  # Test basic dependency resolution
```

#### Branch Does Not Exist
```
[ERROR] Ref develop cannot be resolved
```
```
[ERROR] Base branch 'develop' does not exist
```
The plugin defaults to comparing against the `develop` branch. If your repository uses a different default branch (like `main` or `master`), you need to specify it:

**Solution**: Specify the correct base branch:
```bash
# List all available branches
git branch -a

# Use full coordinates with correct base branch
mvn com.github.aezo:conflict-diff-maven-plugin:1.0.0-SNAPSHOT:analyze -Dconflict-diff.baseBranch=main

# Or with short prefix (if configured)
mvn conflict-diff:analyze -Dconflict-diff.baseBranch=main
```

#### Permission Denied
```
[ERROR] Permission denied while executing git commands
```
**Solution**: Ensure proper Git permissions and authentication:
```bash
git status  # Verify Git access
git fetch  # Test repository access
```

### Debug Mode

Enable debug output for more detailed information:
```bash
mvn conflict-diff:analyze -Dconflict-diff.debug=true -X
```

## License

This project is licensed under the terms specified in the [LICENSE](LICENSE) file.
