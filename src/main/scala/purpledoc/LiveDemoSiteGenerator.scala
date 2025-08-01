package purpledoc

import purpledoc.datatypes.ProjectTree

object LiveDemoSiteGenerator:

  // LinkAll is a flag to build all the shaders before generating the site
  def makeDemoSite(
      projectName: String,
      wd: os.Path,
      liveDemos: os.Path,
      projects: ProjectTree,
      linkAll: Boolean,
      projectKind: ProjectKind,
      config: PurpleDocConfig
  ) =
    println("Building demo site...")
    val projectList = projects.toList.map(_.toMetadata)

    // Build all the examples
    if linkAll then
      println("Building all projects.")
      projectList.foreach { pjt =>
        projectKind match
          case ProjectKind.Tyrian =>
            os.proc("./mill", s"${pjt.millPath}.fullLinkJS").call(cwd = wd)
          case ProjectKind.Indigo =>
            os.proc("./mill", s"${pjt.millPath}.indigoBuildFull").call(cwd = wd)
      }
    else println("Skipping project builds.")

    // Recreate the docs directory
    os.remove.all(liveDemos)
    os.makeDir.all(liveDemos)

    // Generate relative paths
    val projectListRelPaths: List[os.RelPath] =
      projectList.map(_.srcPath)

    // Copy all the built shaders into the right liveDemos directory
    projectListRelPaths.foreach { p =>
      val outPath = liveDemos / p
      os.makeDir.all(outPath)

      projectKind match
        case ProjectKind.Indigo =>
          val buildDir = wd / "out" / p / "indigoBuildFull.dest"

          os.list(buildDir)
            .toList
            .filterNot { p =>
              p.last == "cordova.js" ||
              p.last == "indigo-support.js" ||
              p.last == "index.html"
            }
            .foreach { p =>
              os.copy(p, outPath / p.last)
            }

          // Write a custom index page
          os.write(outPath / "index.html", IndigoIndex.page(outPath.last))

        case ProjectKind.Tyrian =>
          val buildDir = wd / "out" / p / "fullLinkJS.dest"

          os.list(buildDir)
            .toList
            .foreach { p =>
              os.copy(p, outPath / p.last)
            }

          // Write a custom pages
          os.write(outPath / "tyrianapp.js", TyrianTemplates.appJS)
          os.write(outPath / "index.html", TyrianTemplates.index(outPath.last))
    }

    // Build an index page with links to all the sub folders
    os.write(
      liveDemos / "index.html",
      HomePage.page(projectName, projects, config.website.navigationMappings)
    )
