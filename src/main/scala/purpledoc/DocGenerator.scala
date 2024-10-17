package purpledoc

import purpledoc.datatypes.ProjectTree

object DocGenerator:

  def generateDocs(wd: os.Path, projects: ProjectTree): Unit =
    println("Generating docs.")

    val firstProject = projects.toList.headOption.map(_.toMetadata).get

    println("Project at: " + firstProject.srcPath)

    val pageHeader =
      if os.exists(wd / firstProject.srcPath / "README.md") then
        println("> Using README.md as the header.")
        os.read(wd / firstProject.srcPath / "README.md")
      else
        println("> No README.md found, using default header.")
        s"# ${Templates.cleanUpName(firstProject.name)}\n\n"

    // println(pageHeader)

    val scalaFiles = os.walk(wd / firstProject.srcPath).filter(_.ext == "scala")

    // println(scalaFiles.mkString("\n"))

    val comments = scalaFiles.flatMap { file =>
      val lines = os.read.lines(file)
      val comments =
        lines.map(_.trim).filter(_.trim.startsWith("//")).map(_.replaceFirst("//", "").trim)

      comments.toList
    }

    println("Page:")
    println(pageHeader + comments.mkString("\n"))
