#!/bin/bash
set -e

echo "=== Checking formatting (scalafmt) ==="
scala-cli fmt --check .

echo "=== Checking linting (scalafix) ==="
scala-cli --power fix --check --server=false .

echo "=== Running tests ==="
scala-cli test --server=false .

echo "=== All checks passed ==="
