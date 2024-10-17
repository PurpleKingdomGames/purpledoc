package purpledoc

import purpledoc.datatypes.ProjectTree

object DocGenerator:

  def generateDocs(wd: os.Path, output: os.Path, projects: ProjectTree): Unit =
    println("Generating docs.")

    if !os.exists(output) then
      println("ERROR: Output directory does not exist: " + output.toString)
      sys.exit(1)

    projects.toList
      .map(_.toMetadata)
      .foreach: project =>
        println("> Producing docs for: " + project.name)

        val pageHeader =
          if os.exists(wd / project.srcPath / "README.md") then
            os.read(wd / project.srcPath / "README.md")
          else
            println("> No README.md found, using default header.")
            s"# ${Templates.cleanUpName(project.name)}\n\n"

        val scalaFiles = os.walk(wd / project.srcPath).filter(_.ext == "scala")

        val comments = scalaFiles.flatMap { file =>
          val lines = os.read.lines(file)
          val comments =
            lines.map(_.trim).filter(_.trim.startsWith("//")).map(_.replaceFirst("//", "").trim)

          comments.toList
        }

        val contents = pageHeader + comments.mkString("\n\n")

        os.makeDir.all(output / project.srcPath)
        os.write.over(output / project.srcPath / "index.md", contents)

        ()
