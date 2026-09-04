package purpledoc

import org.virtuslab.yaml.*

class PurpleDocConfigTests extends munit.FunSuite:

  val minimalConfig: String =
    """
    |kind: indigo
    |inputs:
    |  staticSite: website
    |outputs:
    |  destination: docs
    |website:
    |  title: Indigo
    |  description: An FP game engine for Scala.
    |  latestRelease: 0.30.0-M6
    |  baseUrl: https://purplekingdomgames.github.io/indigo-docs/
    |  navigationOrder:
    |    - documentation
    |docsRepo:
    |  name: Docs
    |  url: https://github.com/PurpleKingdomGames/indigo-docs
    |  editBaseUrl: https://github.com/PurpleKingdomGames/indigo-docs/blob/main
    |repo:
    |  name: Github
    |  url: https://github.com/PurpleKingdomGames/indigoengine
    |  editBaseUrl: https://github.com/PurpleKingdomGames/indigoengine/blob/main
    |discord:
    |  name: Discord
    |  url: https://discord.gg/3E2r9FwwHu
    |demo:
    |  width: 800
    |  height: 600
    |""".stripMargin

  test("a config without projectLinks or teasers still parses") {
    val actual = minimalConfig.as[PurpleDocConfig].map(_.website)

    assert(clue(actual).isRight)

    assertEquals(actual.map(_.projectLinks), Right(None))
    assertEquals(actual.map(_.teasers), Right(None))
  }

  test("missing projectLinks and teasers are treated as empty lists") {
    val actual = minimalConfig.as[PurpleDocConfig].map(_.website)

    assertEquals(actual.map(_.landingPageProjectLinks), Right(Nil))
    assertEquals(actual.map(_.landingPageTeasers), Right(Nil))
  }

  test("siteUrl joins onto the base url without doubling the slash") {
    val website = minimalConfig.as[PurpleDocConfig].map(_.website)

    // baseUrl in minimalConfig has a trailing slash
    assertEquals(
      website.map(_.siteUrl("live_demos")),
      Right("https://purplekingdomgames.github.io/indigo-docs/live_demos")
    )

    // ...and it should behave the same without one
    assertEquals(
      website.map(_.copy(baseUrl = "https://example.com").siteUrl("live_demos")),
      Right("https://example.com/live_demos")
    )

    assertEquals(
      website.map(_.copy(baseUrl = "https://example.com/").siteUrl("/live_demos")),
      Right("https://example.com/live_demos")
    )
  }

  test("projectLinks and teasers are parsed when present") {
    val input =
      """
      |kind: indigo
      |inputs:
      |  staticSite: website
      |outputs:
      |  destination: docs
      |website:
      |  title: Indigo
      |  description: An FP game engine for Scala.
      |  latestRelease: 0.30.0-M6
      |  baseUrl: https://purplekingdomgames.github.io/indigo-docs/
      |  navigationOrder:
      |    - documentation
      |  projectLinks:
      |    - label: API Docs
      |      url: https://example.com/api/
      |    - label: Live Demos
      |      url: https://example.com/live_demos
      |  teasers:
      |    - title: Purely Functional
      |      description: Immutable game state, no surprises.
      |    - title: Cross Platform
      |      description: Runs anywhere Scala.js runs.
      |docsRepo:
      |  name: Docs
      |  url: https://github.com/PurpleKingdomGames/indigo-docs
      |  editBaseUrl: https://github.com/PurpleKingdomGames/indigo-docs/blob/main
      |repo:
      |  name: Github
      |  url: https://github.com/PurpleKingdomGames/indigoengine
      |  editBaseUrl: https://github.com/PurpleKingdomGames/indigoengine/blob/main
      |discord:
      |  name: Discord
      |  url: https://discord.gg/3E2r9FwwHu
      |demo:
      |  width: 800
      |  height: 600
      |""".stripMargin

    val actual = input.as[PurpleDocConfig].map(_.website)

    val expectedLinks =
      List(
        ProjectLinkConfig("API Docs", "https://example.com/api/"),
        ProjectLinkConfig("Live Demos", "https://example.com/live_demos")
      )

    val expectedTeasers =
      List(
        TeaserConfig("Purely Functional", "Immutable game state, no surprises."),
        TeaserConfig("Cross Platform", "Runs anywhere Scala.js runs.")
      )

    assertEquals(actual.map(_.projectLinks), Right(Option(expectedLinks)))
    assertEquals(actual.map(_.teasers), Right(Option(expectedTeasers)))

    assertEquals(actual.map(_.landingPageProjectLinks), Right(expectedLinks))
    assertEquals(actual.map(_.landingPageTeasers), Right(expectedTeasers))
  }
