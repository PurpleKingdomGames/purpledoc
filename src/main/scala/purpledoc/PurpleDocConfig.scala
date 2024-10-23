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

final case class PurpleDocConfig(inputs: Inputs, outputs: Outputs) derives YamlCodec

final case class Inputs(staticSite: String) derives YamlCodec

final case class Outputs(destination: String) derives YamlCodec
