package purpledoc

import purpledoc.datatypes.ProjectTree
import scala.annotation.tailrec

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

        val comments = scalaFiles.flatMap(extractComments)

        val contents = pageHeader + comments.mkString("\n\n")

        os.makeDir.all(output / project.srcPath)
        os.write.over(output / project.srcPath / "index.md", contents)

        ()

  def extractComments(file: os.Path): List[String] =
    @tailrec
    def rec(remaining: List[String], acc: List[String]): List[String] =
      remaining match
        case Nil =>
          acc

        // Multi-line comment on one line
        case l :: ls if l.contains("/*") && l.contains("*/") =>
          val after   = l.splitAt(l.indexOf("/*") + 2)._2
          val comment = after.splitAt(after.indexOf("*/"))._1.trim

          rec(ls, comment :: acc)

        // Single line comment
        case l :: ls if l.trim.startsWith("//") =>
          rec(ls, l.trim.replaceFirst("//", "").trim :: acc)

        case l :: ls =>
          rec(ls, acc)

    rec(os.read.lines(file).toList, Nil)
