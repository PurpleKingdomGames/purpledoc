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
import laika.io.api.TreeTransformer
import cats.effect.kernel.Resource
import laika.theme.ThemeProvider

object WebsiteGenerator:

  def build(staticSite: os.Path, destination: os.Path, config: PurpleDocConfig): Unit =
    val res =
      transformer(config)
        .use {
          _.fromDirectory(staticSite.toString)
            .toDirectory(destination.toString)
            .transform
        }

    Await.result(
      res.unsafeToFuture(),
      1.minute
    )

  def transformer(config: PurpleDocConfig): Resource[IO, TreeTransformer[IO]] =
    Transformer
      .from(Markdown)
      .to(HTML)
      .using(Markdown.GitHubFlavor, SyntaxHighlighting)
      .parallel[IO]
      .withTheme(theme(config))
      .build

  def theme(config: PurpleDocConfig): ThemeProvider =
    Helium.defaults.all
      .metadata(
        title = Some(config.website.title),
        description = Some(config.website.description),
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
        bgGradient = (Color.hex("29016a"), Color.hex("ffffff"))
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
        homeLink = config.website.topNavLogo match {
          case Some(topNavLogo) =>
            ImageLink.internal(
              Root / "documentation" / "README.md",
              Image.internal(
                Root / "img" / topNavLogo.image,
                alt = Some(config.website.description),
                title = Some(config.website.title),
                width = Some(Length(topNavLogo.width, LengthUnit.px)),
                height = Some(Length(topNavLogo.height, LengthUnit.px))
              )
            )

          case None =>
            IconLink.internal(Root / "documentation" / "README.md", HeliumIcon.home)
        },
        navLinks = Seq(
          ButtonLink.external(config.website.baseUrl + "_live_demos", "Live Demos"),
          ButtonLink.external(config.discord.url, config.discord.name),
          ButtonLink.external(config.repo.url, config.repo.name)
        )
      )
      .site
      .internalCSS(
        Root / "css" / "custom.css",
        condition = _.path != Root / "README.md"
      )
      .site
      .internalCSS(
        Root / "css" / "landing.css",
        condition = _.path == Root / "README.md"
      )
      .site
      .favIcons(
        Favicon.internal(Root / "img" / "favicon.png", sizes = "32x32")
      )
      .site
      .mainNavigation(
        depth = 6,
        includePageSections = false
      )
      .site
      .landingPage(
        logo = config.website.logo.map { logo =>
          Image.internal(
            Root / "img" / logo.image,
            alt = Some(config.website.description),
            title = Some(config.website.title)
          )
        },
        title = Some(config.website.title),
        subtitle = Some(config.website.description),
        latestReleases = Seq(
          ReleaseInfo("Latest Release", config.website.latestRelease)
        ),
        license = Some("MIT"),
        titleLinks = Seq(
          VersionMenu.create(unversionedLabel = "Getting Started"),
          LinkGroup.create(
            IconLink.external(config.repo.url, HeliumIcon.github),
            IconLink.external(config.discord.url, HeliumIcon.chat) // ,
            // IconLink.external("https://twitter.com/indigoengine/", HeliumIcon.twitter)
          )
        ),
        linkPanel = Some(
          LinkPanel(
            "User Guide",
            TextLink.internal(Root / "documentation" / "README.md", "Documentation"),
            TextLink.external(config.website.baseUrl + "/" + "_live_demos", "Live Demos")
          )
        )
      )
      .build
