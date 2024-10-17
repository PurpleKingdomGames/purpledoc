package purpledoc.datatypes

import scala.annotation.tailrec

enum ProjectTree:
  case Branch(name: String, children: List[ProjectTree])
  case Leaf(name: String, path: List[String])
  case Empty

  def prettyPrint(level: Int): String =
    this match
      case ProjectTree.Branch(name, children) =>
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
      case Branch(name, children) => false
      case Leaf(name, _)          => false
      case Empty                  => true

  def hasName(name: String): Boolean =
    this match
      case Branch(n, _) => n == name
      case Leaf(n, _)   => n == name
      case Empty        => false

  def toList: List[ProjectTree.Leaf] =
    this match
      case l @ ProjectTree.Leaf(_, _) =>
        List(l)

      case ProjectTree.Branch(name, children) =>
        children.flatMap(_.toList)

      case ProjectTree.Empty =>
        Nil

object ProjectTree:

  extension (leaf: ProjectTree.Leaf)
    def toMetadata: ProjectMetadata =
      ProjectMetadata(
        leaf.name,
        leaf.path,
        leaf.path.mkString("."),
        os.RelPath(leaf.path.mkString("/"))
      )

  def pathToProjectTree(path: List[String]): ProjectTree =

    def rec(remaining: List[String]): ProjectTree =
      remaining match
        case Nil =>
          ProjectTree.Empty

        case head :: Nil =>
          ProjectTree.Leaf(head, path)

        case head :: tail =>
          ProjectTree.Branch(head, List(rec(tail)))

    rec(path)

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

        case (pt @ ProjectTree.Leaf(name, _)) :: pts if acc.isEmpty =>
          rec(pts, List(pt))

        case (pt @ ProjectTree.Leaf(name, _)) :: pts =>
          rec(pts, acc :+ pt)

        case (pt @ ProjectTree.Branch(name, _)) :: pts if acc.isEmpty =>
          rec(pts, List(pt))

        case (pt @ ProjectTree.Branch(name, _)) :: pts =>
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
            ProjectTree.Branch(nameA, childrenA),
            ProjectTree.Branch(nameB, childrenB)
          ) if nameA == nameB =>
        List(ProjectTree.Branch(nameA, combineTrees(childrenA ++ childrenB)))

      case (
            b1 @ ProjectTree.Branch(nameA, _),
            b2 @ ProjectTree.Branch(nameB, _)
          ) =>
        List(b1, b2)

      case (ProjectTree.Leaf(nameA, p), ProjectTree.Leaf(nameB, _)) if nameA == nameB =>
        List(ProjectTree.Leaf(nameA, p))

      case (ProjectTree.Branch(nameA, childrenA), ProjectTree.Leaf(nameB, p)) =>
        List(ProjectTree.Branch(nameA, childrenA :+ ProjectTree.Leaf(nameB, p)))

      case (ProjectTree.Leaf(nameA, p), ProjectTree.Branch(nameB, childrenB)) =>
        List(ProjectTree.Branch(nameB, ProjectTree.Leaf(nameA, p) :: childrenB))

      case (l1 @ ProjectTree.Leaf(nameA, _), l2 @ ProjectTree.Leaf(nameB, _)) =>
        List(l1, l2)

      case (pt, ProjectTree.Empty) =>
        List(pt)

      case (ProjectTree.Empty, pt) =>
        List(pt)
