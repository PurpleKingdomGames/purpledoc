package purpledoc

import purpledoc.datatypes.ProjectTree
import scala.annotation.tailrec

object DocGenerator:

  def generateDocs(wd: os.Path, generatedDocsOut: os.Path, projects: ProjectTree): Unit =
    println("Generating docs.")

    if !os.exists(generatedDocsOut) then
      os.makeDir.all(generatedDocsOut)

    projects.toList
      .map(_.toMetadata)
      .foreach: project =>
        println("> Producing docs for: " + project.name)

        val pageHeader =
          if os.exists(wd / project.srcPath / "README.md") then
            os.read(wd / project.srcPath / "README.md") + "\n\n"
          else
            println("> No README.md found, using default header.")
            s"# ${Templates.cleanUpName(project.name)}\n\n"

        val scalaFiles = os.walk(wd / project.srcPath).filter(_.ext == "scala")

        val comments = scalaFiles.flatMap(extractComments)

        val contents = pageHeader + comments.mkString("\n\n")

        os.makeDir.all(generatedDocsOut / project.srcPath)
        os.write.over(generatedDocsOut / project.srcPath / "index.md", contents)

        ()

  def extractComments(file: os.Path): List[String] =
    @tailrec
    def rec(remaining: List[String], multiline: List[String], acc: List[String]): List[String] =
      remaining match
        case Nil =>
          acc

        // Multi-line comment on one line
        case l :: ls if l.contains("/*") && l.contains("*/") =>
          val after   = l.splitAt(l.indexOf("/*") + 2)._2
          val comment = after.splitAt(after.indexOf("*/"))._1.trim

          rec(ls, Nil, acc :+ comment)

        // Multi-line comment start
        case l :: ls if l.contains("/*") =>
          val after = l.splitAt(l.indexOf("/*") + 2)._2

          // If we find one multi-line comment start within another,
          // we just drop the first one, considering this an error case.
          // Either it was an error and we just take up to the next close,
          // Or it was a nested comment, and we end up just taking the inner one.
          // Either way, it's degenerate and we don't care.
          rec(ls, after.trim :: Nil, acc)

        // Multi-line comment end
        case l :: ls if l.contains("*/") =>
          val before = l.splitAt(l.indexOf("*/"))._1.trim

          val cleaned =
            if multiline.forall(_.startsWith("*")) then
              multiline.map(_.replaceFirst("\\*", "").trim)
            else multiline.map(_.trim)

          rec(ls, Nil, acc :+ cleaned.mkString("\n") :+ before)

        // Single line comment that denotes the start of a multi-line code grab
        case l :: ls
            if (l.trim.startsWith("//```") || l.trim.startsWith("// ```")) && multiline.isEmpty =>
          rec(ls, l.trim.replaceFirst("//", "").trim :: Nil, acc)

        // Single line comment that denotes the end of a multi-line code grab
        case l :: ls if l.trim.startsWith("//```") || l.trim.startsWith("// ```") =>
          val contents = multiline :+ l.trim.replaceFirst("//", "").trim
          rec(ls, Nil, acc :+ contents.mkString("\n"))

        // Multi-line comment middle (also works for code grabs)
        case l :: ls if multiline.nonEmpty =>
          rec(ls, multiline :+ l.trim, acc)

        // Single line comment
        case l :: ls if l.trim.startsWith("//") =>
          rec(ls, Nil, acc :+ l.trim.replaceFirst("//", "").trim)

        case l :: ls =>
          rec(ls, Nil, acc)

    rec(os.read.lines(file).toList, Nil, Nil).map(_.trim).filterNot(_.isEmpty())
