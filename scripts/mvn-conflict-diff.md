# Maven Dependency Conflict Analyzer - Bash Script

## Overview

For users who prefer a standalone command-line tool or cannot use the Maven plugin, this repository includes a bash script (`scripts/mvn-conflict-diff`) that provides similar functionality to the Maven plugin.

The script compares Maven dependency conflicts between two Git branches by running `mvn dependency:tree` on both branches, extracting conflict information, and showing the differences between them.

## Requirements

- **Git repository**: Project must be a Git repository for branch comparison
- **Maven project**: Must have a valid `pom.xml` file with dependencies  
- **Maven command**: `mvn` command available in PATH
- **Bash shell**: macOS/Linux environment (not compatible with Windows)

## Installation

```bash
# Make the script executable
chmod +x scripts/mvn-conflict-diff

# Add to your PATH for global access
export PATH="$PATH:/path/to/conflict-diff-maven-plugin/scripts"

# Add to your shell profile for permanent access
echo 'export PATH="$PATH:/path/to/conflict-diff-maven-plugin/scripts"' >> ~/.bashrc
# or for zsh
echo 'export PATH="$PATH:/path/to/conflict-diff-maven-plugin/scripts"' >> ~/.zshrc

# Reload your shell or run
source ~/.bashrc  # or ~/.zshrc
```

## Usage

### Basic Syntax
```bash
mvn-conflict-diff [OPTIONS] [BASE_BRANCH]
```

### Common Examples

#### Basic Usage
```bash
# Compare current branch against 'develop' (default)
mvn-conflict-diff

# Compare current branch against 'master'
mvn-conflict-diff master

# Compare current branch against 'main'
mvn-conflict-diff main
```

#### Debug Mode
```bash
# Compare against 'master' with debug output
mvn-conflict-diff master --debug

# Debug mode shows detailed progress information
mvn-conflict-diff --debug
```

#### Multi-Module Projects
```bash
# Analyze only specific modules (by artifactId)
mvn-conflict-diff -pl module1,module2 master

# Analyze single module with debug output
mvn-conflict-diff -pl my-module --debug develop

# Analyze by groupId:artifactId (more precise)
mvn-conflict-diff -pl com.example:my-module master

# Analyze by relative path
mvn-conflict-diff -pl ./backend/api master
```

#### Advanced Options
```bash
# Show help information
mvn-conflict-diff --help

# Debug mode without cleanup (keeps temporary files)
mvn-conflict-diff master --debug --no-clean-up
```

## Options Reference

| Option | Description | Example |
|--------|-------------|---------|
| `BASE_BRANCH` | Base branch to compare against | `mvn-conflict-diff master` |
| `--help` | Show help message and exit | `mvn-conflict-diff --help` |
| `--debug` | Enable debug output with detailed progress | `mvn-conflict-diff --debug` |
| `--no-clean-up` | Skip cleanup of temporary files (requires --debug) | `mvn-conflict-diff --debug --no-clean-up` |
| `-pl <projects>` | Comma-separated list of projects to analyze | `mvn-conflict-diff -pl module1,module2` |

## How It Works

The script follows this workflow:

1. **Validation**: Checks if current branch differs from base branch
2. **Base Branch Analysis**: 
   - Switches to the specified base branch (default: 'develop')
   - Runs `mvn dependency:tree -Dverbose=true`
   - Extracts dependency conflicts using grep and perl
3. **Feature Branch Analysis**:
   - Switches back to the original feature branch
   - Runs the same Maven dependency analysis
4. **Comparison**:
   - Uses `diff` to compare conflict files between branches
   - Displays colored output showing differences
5. **Cleanup**: Removes temporary files (unless `--no-clean-up` is specified)

## Understanding Output

### No Differences Found
```bash
✅ No differences in dependency conflicts found
```

### Differences Found
The script uses `diff` with color output to show:
- **Red lines (-)**: Conflicts present in base branch but not in feature branch
- **Green lines (+)**: New conflicts introduced in feature branch

Example output:
```diff
< 2 org.springframework:spring-core:5.3.21 conflict with 5.3.19
> 1 org.springframework:spring-core:5.3.21 conflict with 5.3.19
> 1 junit:junit:4.13.2 conflict with 4.12
```

This shows:
- Spring conflict count changed from 2 to 1
- New JUnit conflict introduced in feature branch

## Project Module Selection (-pl option)

The `-pl` parameter accepts various formats:

### By Artifact ID
```bash
mvn-conflict-diff -pl my-module master
```
Works when you're in a directory where `pom.xml` contains the module in its modules block.

### By Group ID and Artifact ID  
```bash
mvn-conflict-diff -pl com.example:my-module master
```
More precise, works from any location.

### Multiple Modules
```bash
# Multiple modules by name
mvn-conflict-diff -pl module1,module2,module3 master
```

## Error Handling

The script includes comprehensive error handling:

| Exit Code | Description |
|-----------|-------------|
| 0 | Success - no differences found |
| 1 | Git checkout failed for base branch |
| 2 | Git checkout failed for feature branch |
| 3 | Maven dependency:tree command failed |
| 4 | Dependency tree file was not created |
| 5 | Parsing for conflicts failed |
| 6 | Diff command failed |
| 7-10 | Cleanup failed for various temp files |

## Troubleshooting

### Common Issues

#### Branch Not Found
```bash
❌ Checkout master failed
```
Solution: Ensure the base branch exists locally. Fetch it if needed:
```bash
git fetch origin master:master
```



#### Maven Command Not Found
```bash
❌ Maven dependency:tree command failed
```
Solution: Ensure Maven is installed and available in PATH:
```bash
mvn --version
```

#### Permission Denied
```bash
permission denied: ./scripts/mvn-conflict-diff
```
Solution: Make the script executable:
```bash
chmod +x scripts/mvn-conflict-diff
```

#### Same Branch Warning
```bash
🛑 Feature branch is master, which is the same as the base branch master. Exiting.
```
Solution: This is expected behavior. Switch to a feature branch before running the analysis.

### Debug Mode
When experiencing issues, use debug mode for detailed output:
```bash
mvn-conflict-diff master --debug
```

This shows:
- Branch switching operations
- Maven command execution
- File creation and parsing steps
- Cleanup operations

### Temporary Files
With `--debug --no-clean-up`, the script preserves temporary files for inspection:
- `dependency_tree_<branch>.txt` - Full Maven dependency tree output
- `dependency_tree_conflicts_<branch>.txt` - Extracted conflict information

## Migration from Bash Script to Maven Plugin

| Bash Script | Maven Plugin Equivalent |
|-------------|-------------------------|
| `./mvn-conflict-diff` | `mvn conflict-diff:analyze` |
| `./mvn-conflict-diff master` | `mvn conflict-diff:analyze -Dconflict-diff.baseBranch=master` |
| `./mvn-conflict-diff master --debug` | `mvn conflict-diff:analyze -Dconflict-diff.baseBranch=master -Dconflict-diff.debug=true` |
| `./mvn-conflict-diff -pl module1 master` | `mvn conflict-diff:analyze -pl module1 -Dconflict-diff.baseBranch=master` |

## Advantages of Bash Script vs Maven Plugin

### Bash Script Advantages
- ✅ No plugin installation required
- ✅ Works with any Maven project immediately
- ✅ Standalone tool with no dependencies
- ✅ Simple colored diff output
- ✅ Temporary file inspection capability

### Maven Plugin Advantages  
- ✅ Native Maven integration
- ✅ Better structured output
- ✅ More advanced conflict analysis
- ✅ CI/CD pipeline integration
- ✅ Configurable via pom.xml

Choose the bash script for quick, one-off analysis or when you can't modify the project configuration. Choose the Maven plugin for regular use, CI/CD integration, or when you need more advanced features.
