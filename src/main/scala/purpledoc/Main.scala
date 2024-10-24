package purpledoc

import purpledoc.datatypes.ProjectTree

import mainargs.{main, arg, ParserForMethods, Flag}

/*
TODO:

Variables
- Including
  - Scala version
  - Scala.js version
  - Indigo version
  - Ultraviolet version
  - etc.
- A way to use them in the contents of the site

Self links:
  - Link to this example
  - Edit this page.
  - Anything else?

 */

object Main:

  @main
  def run(
      @arg(
        short = 'i',
        doc = "Path to the project root"
      )
      input: Option[String],
      @arg(
        doc =
          "Boolean flag to specify whether or not to link all Scala.js projects, default is true"
      )
      nolink: Flag
  ) =
    go(input, nolink.value)

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args.toIndexedSeq)

  def go(input: Option[String], noLink: Boolean): Unit =
    val wd =
      input match
        case None =>
          os.pwd

        case Some(in) =>
          if in.startsWith(".") then os.pwd / os.RelPath(in)
          else os.Path(in)

    val config = PurpleDocConfig.load(wd)
    val paths  = Paths(wd, config)

    cleanUp(paths)

    if !os.exists(paths.target) then os.makeDir.all(paths.target)

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

    LiveDemoSiteGenerator.makeDemoSite(wd, paths.liveDemos, projectTree, !noLink)

    DocGenerator.generateDocs(wd, paths.generatedDocs, projectTree, config)

    compileSources(paths.staticSite, paths)

    WebsiteGenerator.build(paths.compiledSources, paths.destination, config)

    println("Done")

  def compileSources(staticSite: os.Path, paths: Paths): Unit = {
    if !os.exists(paths.compiledSources) then os.makeDir.all(paths.compiledSources)

    // Copy the static site to the compiled sources directory
    os.copy.over(
      from = staticSite,
      to = paths.compiledSources,
      followLinks = true,
      replaceExisting = true,
      copyAttributes = false,
      createFolders = true
    )

    // Copy the generated docs to the compiled sources directory
    os.list(paths.generatedDocs).foreach { p =>
      os.copy.into(
        from = p,
        to = paths.compiledSources,
        followLinks = true,
        replaceExisting = true,
        copyAttributes = false,
        createFolders = true,
        mergeFolders = true
      )
    }

    // Copy the live demos to the compiled sources directory
    os.list(paths.liveDemos).foreach { p =>
      os.copy.into(
        from = p,
        to = paths.compiledSources / "demos",
        followLinks = true,
        replaceExisting = true,
        copyAttributes = false,
        createFolders = true,
        mergeFolders = true
      )
    }
  }

  def cleanUp(paths: Paths): Unit =
    if os.exists(paths.target) then
      os.remove.all(paths.target)
      os.makeDir.all(paths.target)

    if os.exists(paths.destination) then
      os.remove.all(paths.destination)
      os.makeDir.all(paths.destination)

final case class Paths(wd: os.Path, config: PurpleDocConfig):

  val target: os.Path          = wd / ".purpledoc"
  val liveDemos: os.Path       = target / "livedemos"
  val generatedDocs: os.Path   = target / "generated-docs"
  val compiledSources: os.Path = target / "compiled-sources"

  val staticSite: os.Path =
    val p =
      if config.inputs.staticSite.startsWith("/") then os.Path(config.inputs.staticSite)
      else wd / config.inputs.staticSite

    if !os.exists(p) then sys.error(s"Static site directory does not exist at: $p")
    else p

  val destination: os.Path =
    if config.outputs.destination.startsWith("/") then os.Path(config.outputs.destination)
    else wd / config.outputs.destination
