package purpledoc

import purpledoc.datatypes.ProjectTree

object SiteGenerator:

  // TODO: Base this on the tree structure so that you can have nicely nested projects lists on the contents page.
  // LinkAll is a flag to build all the shaders before generating the site
  def makeDemoSite(linkAll: Boolean, projects: ProjectTree, wd: os.Path) =
    val projectList = projects.toList.map(_.toMetadata)

    // Build all the shaders
    if linkAll then
      println("Building all projects.")
      projectList.foreach { pjt =>
        os.proc("./mill", s"${pjt.millPath}.buildGameFull").call(cwd = wd)
      }
    else println("Skipping project builds.")

    // Recreate the docs directory
    val docs = wd / "docs"
    os.remove.all(docs)
    os.makeDir.all(docs)

    // Generate relative paths
    val projectListRelPaths: List[os.RelPath] =
      projectList.map(_.srcPath)

    // Copy all the built shaders into the right docs directory
    projectListRelPaths.foreach { p =>
      val outPath = (wd / "docs") / p
      os.makeDir.all(outPath)

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
    }

    // Build an index page with links to all the sub folders
    os.write(
      docs / "index.html",
      HomePage.page(projects)
    )
