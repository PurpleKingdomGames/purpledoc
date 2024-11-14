package purpledoc

import purpledoc.datatypes.ProjectTree
import scala.annotation.tailrec

object DocGenerator:

  def generateDocs(
      wd: os.Path,
      generatedDocsOut: os.Path,
      projects: ProjectTree,
      config: PurpleDocConfig
  ): Unit =
    println("Generating docs.")

    if !os.exists(generatedDocsOut) then os.makeDir.all(generatedDocsOut)

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

        val linksBlock =
          s"""
          |## Example Links
          |
          |  - [Edit this page](${config.docsRepo.editBaseUrl + "/" + project.editHref})
          |  - [Live demo](${config.website.baseUrl + "/demos/" + project.liveDemoHref})
          |
          |""".stripMargin

        val comments = scalaFiles.flatMap(file => extractComments(os.read.lines(file).toList))

        val contents = pageHeader + linksBlock + comments.mkString("\n\n")

        os.makeDir.all(generatedDocsOut / project.srcPath)
        os.write.over(generatedDocsOut / project.srcPath / "README.md", contents)

        ()

    os.walk(generatedDocsOut).foreach { p =>
      if os.isDir(p) && !os.list(p).exists(_.ext == "md") then
        val subdirs =
          os.list(p)
            .filter(os.isDir)
            .toList
            .map(_.last)
            .map(dir => s"[$dir]($dir/README.md)")
            .mkString("  - ", "\n  - ", "")

        val contents =
          s"""# ${p.last.capitalize}
          |
          |$subdirs
          |""".stripMargin

        os.write(p / "README.md", contents)

        if os.exists(p / ".gitkeep") then os.remove(p / ".gitkeep")
    }

  def extractComments(lines: List[String]): List[String] =
    @tailrec
    def rec(
        remaining: List[String],
        multiline: List[String],
        snippet: List[String],
        acc: List[String]
    ): List[String] =
      remaining match
        case Nil =>
          acc

        // Multi-line comment on one line
        case l :: ls if l.contains("/*") && l.contains("*/") =>
          val after   = l.splitAt(l.indexOf("/*") + 2)._2
          val comment = after.splitAt(after.indexOf("*/"))._1.trim

          rec(ls, Nil, Nil, acc :+ comment)

        // Multi-line comment start
        case l :: ls if l.contains("/*") =>
          val after = l.splitAt(l.indexOf("/*") + 2)._2

          // If we find one multi-line comment start within another,
          // we just drop the first one, considering this an error case.
          // Either it was an error and we just take up to the next close,
          // Or it was a nested comment, and we end up just taking the inner one.
          // Either way, it's degenerate and we don't care.
          rec(ls, after.trim :: Nil, Nil, acc)

        // Multi-line comment end
        case l :: ls if l.contains("*/") =>
          val before = l.splitAt(l.indexOf("*/"))._1.trim

          val cleaned =
            if multiline.forall(_.startsWith("*")) then
              multiline.map(_.replaceFirst("\\*", "").trim)
            else multiline.map(_.trim)

          rec(ls, Nil, Nil, acc :+ cleaned.mkString("\n") :+ before)

        // Multi-line comment middle
        case l :: ls if multiline.nonEmpty =>
          rec(ls, multiline :+ l.trim, Nil, acc)

        // Single line comment that denotes the start of a multi-line code/snippet grab
        case l :: ls
            if (l.trim.startsWith("//```") || l.trim.startsWith("// ```")) && snippet.isEmpty =>
          rec(ls, Nil, l.replaceFirst("// ", "").replaceFirst("//", "") :: Nil, acc)

        // Single line comment that denotes the end of a multi-line code/snippet grab
        case l :: ls if l.trim.startsWith("//```") || l.trim.startsWith("// ```") =>
          val contents = cleanCodeBlock(snippet :+ l.replaceFirst("// ", "").replaceFirst("//", ""))

          rec(ls, Nil, Nil, acc :+ contents.mkString("\n"))

        // snippet middle
        case l :: ls if snippet.nonEmpty =>
          rec(ls, Nil, snippet :+ l, acc)

        // Single line comment
        case l :: ls if l.trim.startsWith("//") =>
          rec(ls, Nil, Nil, acc :+ l.trim.replaceFirst("//", "").trim)

        case l :: ls =>
          rec(ls, Nil, Nil, acc)

    rec(lines, Nil, Nil, Nil).map(_.trim).filterNot(_.isEmpty())

  /** Takes a list of strings representing lines of a markdown code block, and returns the code
    * block with excess leading whitespace removed, such that the indentation is preserved.
    */
  def cleanCodeBlock(lines: List[String]): List[String] =
    val indent = lines.map(_.takeWhile(_ == ' ').length).min
    lines.map(_.drop(indent))
