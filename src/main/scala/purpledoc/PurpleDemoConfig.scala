package purpledoc

import org.virtuslab.yaml.*

object PurpleDemoConfig:

  val configFileName: String = "purpledemo.yaml"

  def load(wd: os.Path): Option[PurpleDemoConfig] =
    val configPath = wd / configFileName
    if os.exists(configPath) then
      os.read(configPath).as[PurpleDemoConfig] match
        case Left(value) =>
          println(s"Error parsing purpledoc demo config '$configFileName': $value")
          sys.exit(1)

        case Right(value) =>
          Option(value)
    else
      None

final case class PurpleDemoConfig(
    demo: DemoConfig
) derives YamlCodec
