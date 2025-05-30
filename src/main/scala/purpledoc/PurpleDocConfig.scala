package purpledoc

import org.virtuslab.yaml.*

object PurpleDocConfig:

  val configFileName: String = "purpledoc.yaml"

  def load(wd: os.Path): PurpleDocConfig =
    val configPath = wd / configFileName
    if os.exists(configPath) then
      os.read(configPath).as[PurpleDocConfig] match
        case Left(value) =>
          println(s"Error parsing purpledoc config '$configFileName': $value")
          sys.exit(1)
        case Right(value) =>
          value
    else
      println(s"No '$configFileName' config file found in: ${wd.toString}")
      sys.exit(1)

final case class PurpleDocConfig(
    kind: String,
    inputs: Inputs,
    outputs: Outputs,
    website: WebSiteConfig,
    repo: RepoConfig,
    docsRepo: RepoConfig,
    discord: DiscordConfig
) derives YamlCodec:
  def projectKind: ProjectKind = kind match
    case "indigo" => ProjectKind.Indigo
    case "tyrian" => ProjectKind.Tyrian
    case _        => sys.error(s"Unknown project kind: $kind")

enum ProjectKind derives YamlCodec:
  case Indigo
  case Tyrian

final case class Inputs(staticSite: String) derives YamlCodec

final case class Outputs(destination: String) derives YamlCodec

final case class WebSiteConfig(
    title: String,
    description: String,
    topNavLogo: Option[TopNavLogo],
    logo: Option[Logo],
    latestRelease: String,
    baseUrl: String,
    navigationOrder: List[String]
) derives YamlCodec

final case class TopNavLogo(
    image: String,
    width: Double,
    height: Double
) derives YamlCodec

final case class Logo(
    image: String
) derives YamlCodec

final case class RepoConfig(
    name: String,
    url: String,
    editBaseUrl: String
) derives YamlCodec

final case class DiscordConfig(
    name: String,
    url: String
) derives YamlCodec
