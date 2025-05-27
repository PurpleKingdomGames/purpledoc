package purpledoc.datatypes

import scala.annotation.tailrec

enum ProjectTree:
  case Branch(name: String, path: List[String], children: List[ProjectTree])
  case Leaf(name: String, path: List[String])
  case Empty

  def giveName: String =
    this match
      case Branch(name, _, _) => name
      case Leaf(name, _)      => name
      case Empty              => ""

  def prettyPrint(level: Int): String =
    this match
      case ProjectTree.Branch(name, _, children) =>
        val indent   = "  " * level
        val childStr = children.map(_.prettyPrint(level + 1)).mkString("\n")
        s"$indent$name\n$childStr"

      case ProjectTree.Leaf(name, path) =>
        val indent = "  " * level
        s"$indent$name: $path"

      case ProjectTree.Empty =>
        ""

  def prettyPrint: String =
    prettyPrint(0)

  def isEmpty: Boolean =
    this match
      case Branch(_, _, _) => false
      case Leaf(_, _)      => false
      case Empty           => true

  def hasName(name: String): Boolean =
    this match
      case Branch(n, _, _) => n == name
      case Leaf(n, _)      => n == name
      case Empty           => false

  def toList: List[ProjectTree.Leaf] =
    this match
      case l @ ProjectTree.Leaf(_, _) =>
        List(l)

      case ProjectTree.Branch(name, _, children) =>
        children.flatMap(_.toList)

      case ProjectTree.Empty =>
        Nil

  def sorted: ProjectTree =
    this match
      case b @ Branch(name, _, children) =>
        b.copy(
          children = children.sortBy(_.giveName).map(_.sorted)
        )

      case tree =>
        tree

object ProjectTree:

  extension (leaf: ProjectTree.Leaf)
    def toMetadata: ProjectMetadata =
      ProjectMetadata(
        leaf.name,
        leaf.path,
        leaf.path.mkString("."),
        os.RelPath(leaf.path.mkString("/")),
        leaf.path.mkString("", "/", "/"),
        leaf.path.mkString("/")
      )

  def pathToProjectTree(path: List[String]): ProjectTree =

    def rec(remaining: List[String], used: List[String]): ProjectTree =
      remaining match
        case Nil =>
          ProjectTree.Empty

        case head :: Nil =>
          ProjectTree.Leaf(head, path)

        case head :: tail =>
          val p = used :+ head
          ProjectTree.Branch(head, p, List(rec(tail, p)))

    rec(path, Nil)

  def stringToProjectTree(path: String): ProjectTree =
    pathToProjectTree(path.split("""\.""").toList)

  def combineTrees(trees: List[ProjectTree]): List[ProjectTree] =
    @tailrec
    def rec(
        remaining: List[ProjectTree],
        acc: List[ProjectTree]
    ): List[ProjectTree] =
      remaining match
        case Nil =>
          acc

        case ProjectTree.Empty :: pts =>
          rec(pts, acc)

        case (pt @ ProjectTree.Leaf(_, _)) :: pts if acc.isEmpty =>
          rec(pts, List(pt))

        case (pt @ ProjectTree.Leaf(_, _)) :: pts =>
          rec(pts, acc :+ pt)

        case (pt @ ProjectTree.Branch(_, _, _)) :: pts if acc.isEmpty =>
          rec(pts, List(pt))

        case (pt @ ProjectTree.Branch(name, _, _)) :: pts =>
          // The expected case.
          val res =
            acc.find(_.hasName(name)) match
              case None =>
                acc :+ pt

              case Some(existing) =>
                acc.filterNot(_.hasName(name)) ++ zipTrees(existing, pt)

          rec(pts, res)

    rec(trees, Nil)

  def zipTrees(a: ProjectTree, b: ProjectTree): List[ProjectTree] =
    (a, b) match
      case (
            ProjectTree.Branch(nameA, p, childrenA),
            ProjectTree.Branch(nameB, _, childrenB)
          ) if nameA == nameB =>
        List(ProjectTree.Branch(nameA, p, combineTrees(childrenA ++ childrenB)))

      case (
            b1 @ ProjectTree.Branch(nameA, _, _),
            b2 @ ProjectTree.Branch(nameB, _, _)
          ) =>
        List(b1, b2)

      case (ProjectTree.Leaf(nameA, p), ProjectTree.Leaf(nameB, _)) if nameA == nameB =>
        List(ProjectTree.Leaf(nameA, p))

      case (ProjectTree.Branch(nameA, pathA, childrenA), ProjectTree.Leaf(nameB, p)) =>
        List(ProjectTree.Branch(nameA, pathA, childrenA :+ ProjectTree.Leaf(nameB, p)))

      case (ProjectTree.Leaf(nameA, p), ProjectTree.Branch(nameB, pathB, childrenB)) =>
        List(ProjectTree.Branch(nameB, pathB, ProjectTree.Leaf(nameA, p) :: childrenB))

      case (l1 @ ProjectTree.Leaf(nameA, _), l2 @ ProjectTree.Leaf(nameB, _)) =>
        List(l1, l2)

      case (pt, ProjectTree.Empty) =>
        List(pt)

      case (ProjectTree.Empty, pt) =>
        List(pt)
