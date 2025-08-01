package purpledoc

import scalatags.Text.all._
import purpledoc.datatypes.ProjectTree

object Templates:

  def cleanUpName(name: String, navigationMappings: Option[Map[String, String]]): String =
    navigationMappings
      .flatMap(_.get(name))
      .getOrElse(name.replace("-", " ").capitalize)

object HomePage {

  def page(
      projectName: String,
      projectTree: ProjectTree,
      navigationMappings: Option[Map[String, String]]
  ): String =
    "<!DOCTYPE html>" +
      html(
        head(title := s"$projectName Examples")(
          meta(charset := "UTF-8"),
          link(
            rel  := "stylesheet",
            href := "https://cdn.jsdelivr.net/npm/purecss@3.0.0/build/pure-min.css"
          )
        ),
        body(
          h1(s"$projectName Examples"),
          p(
            "Click on any of the links below."
          ),
          div()(
            ul()(
              projectTreeToHtml(projectTree, navigationMappings)
            )
          )
        )
      )

  def projectTreeToHtml(
      projectTree: ProjectTree,
      navigationMappings: Option[Map[String, String]]
  ): Frag =
    projectTree match {
      case ProjectTree.Branch(name, _, children) =>
        li(Templates.cleanUpName(name, navigationMappings))(
          ul()(
            children.map(projectTreeToHtml(_, navigationMappings))
          )
        )

      case l @ ProjectTree.Leaf(name, _) =>
        val metadata = l.toMetadata
        li()(
          a(href := s"./${metadata.srcPath}")(
            Templates.cleanUpName(name, navigationMappings)
          )
        )

      case ProjectTree.Empty =>
        div()
    }
}

object IndigoIndex {

  def page(pageName: String) =
    "<!DOCTYPE html>" +
      html(
        head(title := pageName)(
          meta(charset := "UTF-8"),
          link(
            rel  := "stylesheet",
            href := "https://cdn.jsdelivr.net/npm/purecss@3.0.0/build/pure-min.css"
          )
        ),
        body(
          div(id := "indigo-container")(),
          script(tpe := "text/javascript", src := "scripts/main.js")(),
          script(tpe := "text/javascript")(
            """IndigoGame.launch('indigo-container')"""
          )
        )
      )

}

object TyrianTemplates {

  // tyrianapp.js
  def appJS: String =
    """
    |import { TyrianApp } from "./main.js";
    |
    |TyrianApp.launch("myapp");
    |""".stripMargin

  // index.html
  def index(pageName: String) =
    "<!DOCTYPE html>" +
      html(
        head(title := pageName)(
          meta(charset := "UTF-8"),
          link(
            rel  := "stylesheet",
            href := "https://cdn.jsdelivr.net/npm/purecss@3.0.0/build/pure-min.css"
          )
        ),
        body(
          div(id := "myapp")(),
          script(tpe := "module", src := "./tyrianapp.js")()
        )
      )

}
