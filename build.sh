#!/bin/bash

# Compile the Scala code to a JAR file
scala-cli --power package . -o purpledoc.jar --assembly --preamble=false -f
