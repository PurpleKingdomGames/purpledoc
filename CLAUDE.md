# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Purpledoc is a CLI tool for building documentation sites for Purple Kingdom Games' open-source Scala projects (Indigo game engine and Tyrian web framework). It generates documentation from working example code and creates live interactive demos.

## Build Commands

```bash
# Build JAR
./build.sh

# Run tests
scala-cli test .

# Run directly (development)
scala-cli . -- --input ./path/to/target/repo --nolink

# Run from JAR
java -jar purpledoc.jar --input . --nolink

# Partial build (faster - only specific examples)
purpledoc -i . -p colour,noise
```

## Code Formatting

Uses Scalafmt (configured in `.scalafmt.conf`). Key settings: Scala 3, maxColumn=100.

## Architecture

The build pipeline flows through these stages:

1. **Configuration** (`PurpleDocConfig.scala`, `PurpleDemoConfig.scala`) - Load `purpledoc.yaml` from target project root and optional per-example `purpledemo.yaml` overrides
2. **Project Discovery** (`MillProjectLister.scala`) - Run Mill commands to list all buildable projects
3. **Tree Building** (`datatypes/ProjectTree.scala`) - Convert flat project list into hierarchical tree structure
4. **Demo Generation** (`LiveDemoSiteGenerator.scala`) - Build Scala.js projects via Mill, generate HTML demo pages
5. **Doc Extraction** (`DocGenerator.scala`) - Parse Scala files, extract comments (single-line, multi-line, scaladoc) using regex, generate Markdown
6. **Website Generation** (`WebsiteGenerator.scala`) - Use Laika to transform Markdown to HTML with Helium theme

### Key Data Types

- `ProjectTree` (enum: Branch/Leaf/Empty) - Hierarchical representation of Mill projects
- `ProjectMetadata` - Name, paths, and href info for each project
- `Paths` - Working directory paths (.purpledoc/, livedemos/, generated-docs/, etc.)

### Templates

`Templates.scala` contains HTML templates for Indigo and Tyrian demo pages, using Scalatags for type-safe HTML generation.

## Dependencies

- **os-lib** - File I/O and process execution
- **scalatags** - HTML generation
- **mainargs** - CLI argument parsing
- **scala-yaml** - YAML config parsing
- **laika-io** - Markdown to HTML transformation
- **munit** - Testing

## Target Project Requirements

Projects being documented must have:
1. A Mill build system
2. A `purpledoc.yaml` at the root (specifies kind: indigo/tyrian, paths, website metadata)
3. Indigo or Tyrian example projects arranged in a tree structure
