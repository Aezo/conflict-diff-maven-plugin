# Maven Dependency Conflict Analyzer Plugin

A Maven plugin that compares dependency conflicts between Git branches to help identify dependency changes introduced by feature branches.

## Table of Contents

- [Description](#description)
- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Usage](#usage)
- [Output](#output)
- [Alternative Tools](#alternative-tools)
- [Integration Examples](#integration-examples)
- [Contributing](#contributing)
- [Troubleshooting](#troubleshooting)
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

See [PLUGIN-USAGE.md](PLUGIN-USAGE.md#installation) for detailed installation instructions.

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

For detailed usage instructions, configuration options, and examples, see [PLUGIN-USAGE.md](PLUGIN-USAGE.md).

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


## Output

For detailed output examples and explanations, see [PLUGIN-USAGE.md](PLUGIN-USAGE.md#output).

## Alternative Tools

### Bash Script (mvn-conflict-diff)

For users who prefer a standalone command-line tool or cannot use the Maven plugin, this repository also includes a bash script that provides similar functionality.

📖 **[Complete bash script documentation →](scripts/mvn-conflict-diff.md)**

**Quick Start:**
```bash
# Make executable and run
chmod +x scripts/mvn-conflict-diff

# (Optional) Add to your PATH for global access
export PATH="$PATH:/path/to/conflict-diff-maven-plugin/scripts"

# Run the script
mvn-conflict-diff
```

## Integration Examples

For CI/CD pipeline integration examples and Maven lifecycle binding, see [PLUGIN-USAGE.md](PLUGIN-USAGE.md#cicd-integration).

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

For basic troubleshooting steps, see [PLUGIN-USAGE.md](PLUGIN-USAGE.md#troubleshooting).

### Additional Common Issues

#### Plugin Prefix Not Recognized
```
[ERROR] No plugin found for prefix 'conflict-diff' in the current project
```

**Solution 1**: Use the full plugin coordinates:
```bash
mvn com.github.aezo:conflict-diff-maven-plugin:1.0.0-SNAPSHOT:analyze
```

**Solution 2**: Add the plugin group to your Maven settings (`~/.m2/settings.xml`):
```xml
<pluginGroups>
   <pluginGroup>com.github.aezo</pluginGroup>
</pluginGroups>
```

#### Branch Does Not Exist
The plugin defaults to comparing against the `develop` branch. If your repository uses a different default branch (like `main` or `master`), specify it:
```bash
mvn conflict-diff:analyze -Dconflict-diff.baseBranch=main
```

## License

This project is licensed under the terms specified in the [LICENSE](LICENSE) file.
