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
            println("  - No README.md found, using default header.")
            s"# ${Templates.cleanUpName(project.name, config.website.navigationMappings)}\n\n"

        val configWithOverrides =
          PurpleDemoConfig.load(wd / project.srcPath) match
            case Some(demo) =>
              println("  - Config overrides found.")
              config.overrideDemoConfig(demo)

            case None =>
              config

        val scalaFiles = os.walk(wd / project.srcPath).filter(_.ext == "scala")

        val demoId   = "live-demo"
        val demoHref = configWithOverrides.website.baseUrl + "live_demos/" + project.liveDemoHref + "index.html"

        val demoDivStyles =
          List(
            "border:2px solid #e400ff",
            s"width:${configWithOverrides.demo.width}px",
            s"height:${configWithOverrides.demo.height}px",
            "background:#a888db33",
            "color: #e400ff",
            "display:flex",
            "align-items:center",
            "justify-content:center",
            "cursor:pointer"
          ).mkString("", ";", ";")

        val demoBlock =
          if configWithOverrides.demo.isHidden then ""
          else {
            s"""
            |## Demo
            |
            |<div id="$demoId" style="$demoDivStyles">
            |  ▶  Click to play
            |</div>
            |
            |<script>
            |  document.getElementById("$demoId").addEventListener("click", function() {
            |    this.innerHTML = '<iframe width="${configWithOverrides.demo.width - 4}" height="${configWithOverrides.demo.height - 4}" src="$demoHref" frameborder="0" allow="autoplay; encrypted-media" scrolling="no"></iframe>';
            |  });
            |</script>
            |
            |""".stripMargin
          }

        val demoLink =
          if configWithOverrides.demo.isHidden then
            println("  - Demo will be hidden.")
            ""
          else s"""  - [Live demo](${demoHref})"""

        val linksBlock =
          s"""
          |## Links
          |
          |  - [View example code](${configWithOverrides.docsRepo.editBaseUrl + "/" + project.editHref})
          |${demoLink}
          |
          |""".stripMargin

        val comments = scalaFiles.flatMap(file => extractComments(os.read.lines(file).toList))

        val contents = compileMarkdown(pageHeader, demoBlock, linksBlock, comments.toList)

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
          s"""# ${Templates.cleanUpName(p.last, config.website.navigationMappings)}
          |
          |$subdirs
          |""".stripMargin

        os.write(p / "README.md", contents)

        if os.exists(p / ".gitkeep") then os.remove(p / ".gitkeep")
    }

    os.walk(generatedDocsOut).foreach { p =>
      if os.isDir(p) && !os.list(p).exists(p => p.baseName == "directory" && p.ext == "conf") then
        val navigationOrder =
          os.list(p)
            .map(_.last)
            .filterNot(_ == "README.md")
            .toList
            .sorted

        if navigationOrder.nonEmpty then
          val directoryConf =
            s"""
            |laika.navigationOrder = [
            |${navigationOrder.mkString("  ", "\n  ", "")}
            |]
            |""".stripMargin

          os.write.over(p / "directory.conf", directoryConf)
    }

  def compileMarkdown(
      pageHeader: String,
      demoBlock: String,
      linksBlock: String,
      comments: List[String]
  ): String =
    pageHeader + demoBlock + linksBlock + comments.mkString("\n\n")

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

        // Multi-line scaladoc comment on one line
        case l :: ls if l.contains("/**") && l.contains("*/") =>
          val after   = l.splitAt(l.indexOf("/**") + 3)._2
          val comment = after.splitAt(after.indexOf("*/"))._1.trim

          rec(ls, Nil, Nil, acc :+ comment)

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

          rec(ls, Nil, Nil, acc :+ contents)

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
  def cleanCodeBlock(lines: List[String]): String =
    // Filter out code fence lines and empty lines when calculating minimum indent
    val contentLines = lines.filterNot(line => line.trim.startsWith("```") || line.trim.isEmpty)

    if contentLines.isEmpty then lines.mkString("\n") // If no content lines, return as-is
    else
      val indent = contentLines.map(_.takeWhile(_ == ' ').length).min
      val cleanedLines = lines.map { line =>
        if line.trim.isEmpty then line // Preserve empty lines as-is
        else if line.trim.startsWith("```") then
          line.trim            // Code fence lines should not be indented in markdown
        else line.drop(indent) // Remove baseline indentation from content lines
      }
      cleanedLines.mkString("\n")
