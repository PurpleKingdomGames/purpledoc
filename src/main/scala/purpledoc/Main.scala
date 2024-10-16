package purpledoc

import purpledoc.datatypes.ProjectTree

import mainargs.{main, arg, ParserForMethods, Flag}

/*
TODO:

As application...
- Move to separate repo. DONE
- Build jar / native
- Setup alias

Purpledocs
- Config in repo
- Config load / validation
- Input
- Live site output
- Docs output

Live examples site
- Live examples site contents tree should be nicely nested, based on the project tree.

Documentation scraper
- Find READMEs
- Front matter? What do docusaurus and Hugo ask for?
- Guess the project title / fallback based on project name, if not in front matter or README.md
- Collect all scala files
- scrape comments
- scape snippets
- Output markdown file
  - Front matter (format?)
  - README contents
  - Append comments and snippets

 */

object Main:

  @main
  def run(
      @arg(short = 'i', doc = "Path to the project root")
      input: String,
      @arg(
        doc =
          "Boolean flag to specify whether or not to link all Scala.js projects, default is true"
      )
      nolink: Flag
  ) =
    go(input, nolink.value)

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args)

  def go(input: String, noLink: Boolean): Unit =
    val wd =
      if input.startsWith(".") then os.pwd / os.RelPath(input)
      else os.Path(input)

    val projectList = MillProjectLister.buildProjectList(wd)
    val trees =
      ProjectTree.combineTrees(projectList.map(ProjectTree.stringToProjectTree))

    println("Found projects:")
    println(trees.map(_.prettyPrint).mkString("\n"))

    // println("Leaf nodes:")
    // println(trees.flatMap(_.toList).map(_.name).mkString("\n"))

    // println("Metadata:")
    // println(trees.flatMap(_.toList).map(_.toMetadata(wd)).mkString("\n"))

    println("Building demo site...")
    SiteGenerator.makeDemoSite(!noLink, projectList, wd)

    println("Done")
