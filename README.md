# Purpledoc

Purpledoc is an internal command line tool for building the documentation sites for Purple Kingdom Games' open source projects.

It isn't designed for general purpose use, but the tool is open source in case anyone wants to adapt and make use of it.

The idea is to aim for maximum maintainability and correctness, following the agile manifesto value:

> Working software over comprehensive documentation

The documentation produced, then, is split into three parts:

1. Conceptual and unavoidably manual documentation, that covers long lived topics that remain largely true across many or all releases.
2. Documentation generated from working examples.
3. Live demos built from the working examples.

Point (2) was really the starting notion of the project. The idea is that rather than writing documentation and putting code in it that must be checked by tools like MDoc, we write real example projects that have documentation in them.

## Prerequisites

The repo you plan to document needs three things:

1. It must be a Mill project.
2. It must have a purpledoc.yaml file at its root.
3. It must contain Indigo (or soon Tyrian) projects, arranged in a tree.

## Running purpledoc

Start by checking out this repo, and then use one of the following methods.

### Method 1: It's just Scala-CLI, after all

In your terminal, navigate to the repo root, and run as follows:

```
scala-cli . -- --input ./relative/path/to/target/repo --nolink
```

The `--nolink` parameter tells purpledoc that the project has been built, and not to bother doing that.

### Method 2: CLI

Add the following to your `.zshrc` file (assuming ZSH):

```
alias purpledoc="java -jar <path to repo>/purpledoc/purpledoc.jar"
```

Now you can run `purpledoc --input . --nolink` or `purpledoc -i .` from any directory.

### Method 3: Native app

Refer to the scala-cli docs to make a native GraalVM image and alias it as above. Technically produces faster start times, but method 2 is not slow.

## Partial builds

Once you start to build up a lot of examples in your project, waiting for a full rebuild can take quite a while and be rather boring. To speed things up, you can perform a partial build of just the examples you care about at the moment using the `--partial` or `-p` flag, as follows:

```
purpledoc -i . -p colour,noise
```

The example above will build any example project / module with the word `colour` or `noise` in its path.