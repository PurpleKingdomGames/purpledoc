package purpledoc.datatypes

final case class ProjectMetadata(
    name: String,
    path: List[String],
    millPath: String,
    srcPath: os.RelPath,
    liveDemoHref: String,
    editHref: String
)
