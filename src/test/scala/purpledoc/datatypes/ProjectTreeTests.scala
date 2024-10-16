package purpledoc.datatypes

class ProjectTreeTests extends munit.FunSuite:

  test("pathToProjectTree") {
    val path = List("a", "b", "c")

    val actual =
      ProjectTree.pathToProjectTree(path)

    val expected =
      ProjectTree.Branch(
        "a",
        List(
          ProjectTree.Branch(
            "b",
            List(
              ProjectTree.Leaf("c", List("a", "b", "c"))
            )
          )
        )
      )

    assertEquals(actual, expected)
  }

  test("combineTrees") {

    val pathA = List("a", "b", "c", "d")
    val pathB = List("a", "b", "c", "e")
    val pathC = List("a", "b", "f", "g")
    val pathD = List("a", "b", "h")

    val trees = List(
      ProjectTree.pathToProjectTree(pathA),
      ProjectTree.pathToProjectTree(pathB),
      ProjectTree.pathToProjectTree(pathC),
      ProjectTree.pathToProjectTree(pathD)
    )

    val actual =
      ProjectTree.combineTrees(trees)

    val expected =
      List(
        ProjectTree.Branch(
          "a",
          List(
            ProjectTree.Branch(
              "b",
              List(
                ProjectTree.Branch(
                  "c",
                  List(
                    ProjectTree.Leaf("d", pathA),
                    ProjectTree.Leaf("e", pathB)
                  )
                ),
                ProjectTree.Branch(
                  "f",
                  List(
                    ProjectTree.Leaf("g", pathC)
                  )
                ),
                ProjectTree.Leaf("h", pathD)
              )
            )
          )
        )
      )

    assertEquals(actual, expected)
  }

  test("combine") {
    val paths =
      List(
        // "shaders.fragment.basics.colours",
        "fragment.sdf.circle",
        "fragment.shapes.doughnut",
        "fragment.shapes.metaballs"
      ).map(ProjectTree.stringToProjectTree)

    val actual =
      ProjectTree.combineTrees(paths)

    val expected =
      List(
        ProjectTree.Branch(
          "fragment",
          List(
            ProjectTree.Branch(
              "sdf",
              List(
                ProjectTree.Leaf("circle", List("fragment", "sdf", "circle"))
              )
            ),
            ProjectTree.Branch(
              "shapes",
              List(
                ProjectTree
                  .Leaf("doughnut", List("fragment", "shapes", "doughnut")),
                ProjectTree.Leaf(
                  "metaballs",
                  List("fragment", "shapes", "metaballs")
                )
              )
            )
          )
        )
      )

    assertEquals(actual, expected)
  }
