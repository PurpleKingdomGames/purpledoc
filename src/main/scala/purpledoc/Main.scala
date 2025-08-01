package purpledoc

import purpledoc.datatypes.ProjectTree

import mainargs.{main, arg, ParserForMethods, Flag}

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
      nolink: Flag,
      @arg(
        short = 'p',
        doc = "Partial build. A comma separated list of project names to build."
      )
      partial: Option[String]
  ) =
    go(input, nolink.value, partial)

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args.toIndexedSeq)

  def go(input: Option[String], noLink: Boolean, partial: Option[String]): Unit =
    val projectFilter: List[String] =
      partial match
        case None =>
          Nil

        case Some(p) =>
          p.split(",").toList.map(_.trim)

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

    val projectList = MillProjectLister.buildProjectList(wd, projectFilter)

    val trees =
      ProjectTree.combineTrees(projectList.map(ProjectTree.stringToProjectTree))

    val projectTree =
      trees match
        case Nil =>
          ProjectTree.Empty

        case head :: Nil =>
          head

        case head :: tail =>
          ProjectTree.Branch("root", List("/"), head :: tail)

    val sortedProjectTree =
      projectTree.sorted

    LiveDemoSiteGenerator.makeDemoSite(
      config.website.title,
      wd,
      paths.liveDemos,
      sortedProjectTree,
      !noLink,
      config.projectKind,
      config
    )

    DocGenerator.generateDocs(wd, paths.generatedDocs, projectTree, config)

    compileSources(paths.staticSite, paths, config.website.navigationOrder)

    WebsiteGenerator.build(paths.compiledSources, paths.destination, config)

    println("Done")

  def compileSources(staticSite: os.Path, paths: Paths, navigationOrder: List[String]): Unit = {
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
        to = paths.compiledSources / "live_demos",
        followLinks = true,
        replaceExisting = true,
        copyAttributes = false,
        createFolders = true,
        mergeFolders = true
      )
    }

    val directoryConf =
      s"""
      |laika.navigationOrder = [
      |${navigationOrder.mkString("  ", "\n  ", "")}
      |]
      |""".stripMargin

    os.write.over(paths.compiledSources / "directory.conf", directoryConf)
  }

  def cleanUp(paths: Paths): Unit =
    if !os.exists(paths.target) then os.makeDir.all(paths.target)
    else
      os.list(paths.target).foreach { p =>
        os.remove.all(p)
      }

    if !os.exists(paths.destination) then os.makeDir.all(paths.destination)
    else
      os.list(paths.destination).foreach { p =>
        os.remove.all(p)
      }

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
