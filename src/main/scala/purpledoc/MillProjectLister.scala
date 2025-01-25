package purpledoc

object MillProjectLister {

  def buildProjectList(wd: os.Path, projectFilter: List[String]): List[String] =
    // Extract all sub-projects
    val findProjects = os
      .proc(
        "./mill",
        "resolve",
        "__.fullLinkJS"
      )
      .spawn(cwd = wd)

    val filterOutTestProjects = os
      .proc(
        "grep",
        "-v",
        "test"
      )
      .spawn(cwd = wd, stdin = findProjects.stdout)

    val cleanUpNames = os
      .proc(
        "sed",
        "s/.fullLinkJS//"
      )
      .spawn(cwd = wd, stdin = filterOutTestProjects.stdout)

    val theList =
      LazyList
        .continually(cleanUpNames.stdout.readLine())
        .takeWhile(_ != null)
        .toList

    if projectFilter.isEmpty then theList
    else theList.filter(p => projectFilter.exists(pf => p.contains(pf)))

}
