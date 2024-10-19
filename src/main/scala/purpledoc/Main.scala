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
- DONE - Config in repo
- DONE - Config load / validation
- DONE - Live site output
- DONE - Docs output

Live examples site
- DONE - Live examples site contents tree should be nicely nested, based on the project tree.

Documentation scraper
- DONE - Find READMEs
- Front matter? What do docusaurus and Hugo ask for?
- DONE - Guess the project title / fallback based on project name, if not in front matter or README.md
- DONE - Collect all scala files
- DONE - scrape comments
- DONE scape snippets
- DONE - Output markdown file
  - Front matter (format?)
  - DONE - README contents
  - DONE - Append comments
  - DONE - ...and snippets

Static pages
- Define a directory for static pages in the config
- Copy all files from that directory to the output directory (configurable)

Static asset copying
- Define a directory for static assets in the config
- Copy all files from that directory to the output directory (configurable)

Variables
- Including
  - Scala version
  - Scala.js version
  - Indigo version
  - Ultraviolet version
  - etc.
- A way to use them in the contents of the site

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

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args.toIndexedSeq)

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

    val config = Config.load(wd)

    SiteGenerator.makeDemoSite(wd, projectTree, !noLink)

    DocGenerator.generateDocs(wd, wd / config.outputs.docs, projectTree)

    println("Done")
