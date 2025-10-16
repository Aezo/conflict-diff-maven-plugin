# Maven Dependency Conflict Analyzer Plugin

🔍 Compare Maven dependency conflicts between Git branches to identify changes introduced by feature branches.

## Overview

This plugin analyzes how your feature branch changes affect Maven dependency resolution by comparing conflicts between your current branch and a base branch. Perfect for understanding dependency impacts before merging.

**Key Benefits:**
- 🌿 **Branch Comparison** - Compare conflicts between any two branches  
- 📊 **Detailed Analysis** - Shows conflict counts and details
- 🚀 **CI/CD Ready** - Easy integration with build pipelines
- ⚙️ **Configurable** - Flexible configuration options

## Quick Start

**Prerequisites:** Java 8+, Maven 3.6.3+, Git repository

1. **Install the plugin:**
   ```bash
   git clone https://github.com/aezo/conflict-diff-maven-plugin.git
   cd conflict-diff-maven-plugin
   mvn clean install
   ```

2. **Run analysis in your project:**
   ```bash
   cd /path/to/your/maven/project
   mvn com.github.aezo:conflict-diff-maven-plugin:1.0.0-SNAPSHOT:analyze -Dconflict-diff.baseBranch=main
   ```

3. **Optional - Add plugin group to `~/.m2/settings.xml` for shorter commands:**
   ```xml
   <pluginGroups>
      <pluginGroup>com.github.aezo</pluginGroup>
   </pluginGroups>
   ```
   Then use: `mvn conflict-diff:analyze -Dconflict-diff.baseBranch=main`

## Usage Examples

### Basic Commands
```bash
# Compare current branch with main
mvn conflict-diff:analyze -Dconflict-diff.baseBranch=main

# Enable debug output
mvn conflict-diff:analyze -Dconflict-diff.debug=true

# Multi-module projects - specific modules
mvn conflict-diff:analyze -pl module1,module2
```

### Configuration Options
- `baseBranch` - Branch to compare against (default: `develop`)
- `debug` - Enable verbose output (default: `false`)
- `skipConflictDiff` - Skip plugin execution (default: `false`)

**💡 For detailed configuration, examples, and CI/CD integration:** [PLUGIN-USAGE.md](PLUGIN-USAGE.md)

## Alternative: Bash Script

Prefer command-line tools? Use the included bash script:
```bash
chmod +x scripts/mvn-conflict-diff
export PATH="${PATH}:/path/to/scripts/mvn-conflict-diff"
mvn-conflict-diff
```
📖 [Script documentation](scripts/mvn-conflict-diff.md)

## Common Issues

**Plugin not found?** Use full coordinates:
```bash
mvn com.github.aezo:conflict-diff-maven-plugin:1.0.0-SNAPSHOT:analyze
```

**Wrong default branch?** Specify your branch:
```bash
mvn conflict-diff:analyze -Dconflict-diff.baseBranch=main
```

💡 **More troubleshooting:** [PLUGIN-USAGE.md](PLUGIN-USAGE.md#troubleshooting)

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`) 
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

**Development:**
```bash
git clone https://github.com/your-username/conflict-diff-maven-plugin.git
cd conflict-diff-maven-plugin
mvn clean install
```

## License

Licensed under the terms in [LICENSE](LICENSE) file.
