package purpledoc.datatypes

final case class ProjectMetadata(
  path: List[String],
  millPath: String,
  srcPath: os.Path,
)
