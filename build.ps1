# Compile the Scala code to a JAR file
scala-cli --power package --server=false . -o purpledoc.jar --assembly --preamble=false -f
