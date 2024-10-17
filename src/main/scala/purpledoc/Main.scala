package purpledoc

import purpledoc.datatypes.ProjectTree

import mainargs.{main, arg, ParserForMethods, Flag}

/*
TODO:

As application...
- DONE - Move to separate repo
- DONE - Build jar / native
- DONE - Setup alias

Purpledocs
- Config in repo
- Config load / validation
- Live site output
- Docs output

Live examples site
- DONE - Live examples site contents tree should be nicely nested, based on the project tree.

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
      @arg(
        short = 'i',
        doc = "Path to the project root"
      ) // TODO: Can this be set up to not need the flag?
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
    val projectTree =
      trees match
        case Nil =>
          ProjectTree.Empty

        case head :: Nil =>
          head

        case head :: tail =>
          ProjectTree.Branch("root", tail)

    println("Found projects:")
    println(projectTree.prettyPrint)

    // println("Leaf nodes:")
    // println(trees.flatMap(_.toList).map(_.name).mkString("\n"))

    // println("Metadata:")
    // println(trees.flatMap(_.toList).map(_.toMetadata(wd)).mkString("\n"))

    println("Building demo site...")
    SiteGenerator.makeDemoSite(!noLink, projectTree, wd)

    println("Done")
