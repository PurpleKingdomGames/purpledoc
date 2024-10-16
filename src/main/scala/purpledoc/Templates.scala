package purpledoc

import scalatags.Text.all._

object HomePage {

  def page(projectList: List[String]) =
    "<!DOCTYPE html>" +
      html(
        head(title := "Ultraviolet Examples")(
          meta(charset := "UTF-8"),
          link(
            rel := "stylesheet",
            href := "https://cdn.jsdelivr.net/npm/purecss@3.0.0/build/pure-min.css"
          )
        ),
        body(
          h1("Ultraviolet Examples"),
          p(
            "Click on any of the links below."
          ),
          projectList
            .map(_.split("/").toList.drop(1))
            .map {
              case category :: names =>
                category -> names.mkString("/")

              case unexpected =>
                throw new Exception(
                  "Failed trying to convert this to a tuple: " + unexpected
                )
            }
            .groupBy(_._1)
            .toList
            .map(p => p._1 -> p._2.map(_._2))
            .map { case (category, sections) =>
              div()(
                p(category.capitalize),
                ul()(
                  sections.map { prj =>
                    li(
                      a(href := s"./shaders/$category/$prj")(
                        prj.replace("-", " ").capitalize
                      )
                    )
                  }
                )
              )
            }
        )
      )

}

object IndigoIndex {

  def page(pageName: String) =
    "<!DOCTYPE html>" +
      html(
        head(title := pageName)(
          meta(charset := "UTF-8"),
          link(
            rel := "stylesheet",
            href := "https://cdn.jsdelivr.net/npm/purecss@3.0.0/build/pure-min.css"
          )
        ),
        body(
          div(id := "indigo-container")(),
          script(tpe := "text/javascript", src := "scripts/main.js")(),
          script(tpe := "text/javascript")(
            """IndigoGame.launch('indigo-container')"""
          ),
          p("Hit 'f' for fullscreen.")
        )
      )

}
