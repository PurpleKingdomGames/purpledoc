package purpledoc

import cats.effect.IO
import laika.config.SyntaxHighlighting
import laika.config.PrettyURLs
import laika.theme.config.Color
import laika.theme.config.Color._
import laika.helium.config.ColorQuintet
import laika.helium.config._
import laika.ast.Path.Root
import laika.ast.Image
import laika.ast.Length
import laika.ast.LengthUnit
import laika.api._
import laika.format._
import laika.io.syntax._
import laika.helium.Helium
import laika.io.model.RenderedTreeRoot
import laika.config.{LinkConfig, TargetDefinition}
import java.time.OffsetDateTime
import scala.concurrent.Await
import scala.concurrent.duration._

import cats.effect.unsafe.implicits.global
import scala.concurrent.ExecutionContext.Implicits.global

object WebsiteGenerator:

  def build(staticSite: os.Path, destination: os.Path): Unit =
    val res =
      transformer
        .use {
          _.fromDirectory(staticSite.toString)
            .toDirectory(destination.toString)
            .transform
        }

    Await.result(
      res.unsafeToFuture(),
      1.minute
    )

  def transformer =
    Transformer
      .from(Markdown)
      .to(HTML)
      .using(Markdown.GitHubFlavor)
      .parallel[IO]
      .withTheme(theme)
      .build

  def theme =
    Helium.defaults.all
      .metadata(
        title = Some("Purple Kingdom Games"),
        description = Some("???"),
        identifier = Some(""),
        authors = Seq(),
        language = Some("en"),
        datePublished = Some(OffsetDateTime.now),
        version = Some("1.0.0")
      )
      .all
      .themeColors(
        primary = Color.hex("29016a"),
        secondary = Color.hex("9003c8"),
        primaryMedium = Color.hex("a888db"),
        primaryLight = Color.hex("e4e4e4"),
        text = Color.hex("5f5f5f"),
        background = Color.hex("ffffff"),
        bgGradient = (Color.hex("095269"), Color.hex("007c99"))
      )
      .site
      .darkMode
      .themeColors(
        primary = Color.hex("29016a"),
        secondary = Color.hex("9003c8"),
        primaryMedium = Color.hex("a888db"),
        primaryLight = Color.hex("e4e4e4"),
        text = Color.hex("5f5f5f"),
        background = Color.hex("ffffff"),
        bgGradient = (Color.hex("29016a"), Color.hex("ffffff"))
      )
      .all
      .syntaxHighlightingColors(
        base = ColorQuintet(
          hex("2a3236"),
          hex("8c878e"),
          hex("b2adb4"),
          hex("bddcee"),
          hex("e8e8e8")
        ),
        wheel = ColorQuintet(
          hex("e28e93"),
          hex("ef9725"),
          hex("ffc66d"),
          hex("7fb971"),
          hex("4dbed4")
        )
      )
      .site
      .topNavigationBar(
        homeLink = ImageLink.internal(
          Root / "README.md",
          Image.internal(
            Root / "img" / "tyrian-horizontal.svg",
            alt = Some("Tyrian, an Elm inspired frontend framework for Scala.js."),
            title = Some("Tyrian"),
            width = Some(Length(150.0, LengthUnit.px)),
            height = Some(Length(50.0, LengthUnit.px))
          )
        ),
        navLinks = Seq(
          ButtonLink.external("https://discord.gg/b5CD47g", "Discord"),
          ButtonLink.external("/api", "API"),
          ButtonLink.external(
            "https://github.com/PurpleKingdomGames/tyrian",
            "Github"
          )
        )
      )
      .site
      .tableOfContent(title = "Contents", depth = 2)
      .site
      .internalCSS(Root / "css" / "custom.css")
      .site
      .favIcons(
        Favicon.internal(Root / "img" / "favicon.png", sizes = "32x32")
      )
      .site
      .mainNavigation(
        depth = 6,
        includePageSections = false
      )
      .build
