package purpledoc

class DocGeneratorTests extends munit.FunSuite:

  test("single line comment") {
    val input =
      """
      |// This is a comment
      |val x = 1
      |""".stripMargin

    val lines =
      input.split("\n").toList

    val actual = DocGenerator.extractComments(lines)

    val expected =
      List(
        "This is a comment"
      )

    assertEquals(actual, expected)
  }

  test("multiline on one line") {
    val input =
      """
      |val y = 2 /*something */
      |/* This is a comment */
      |val x = 1
      |""".stripMargin

    val lines =
      input.split("\n").toList

    val actual = DocGenerator.extractComments(lines)

    val expected =
      List(
        "something",
        "This is a comment"
      )

    assertEquals(actual, expected)
  }

  test("multiline comment") {
    val input =
      """
      |val y = 2
      |/*
      |This is a comment
      |and it continues
      |across multiple lines
      |*/
      |val x = 1
      |""".stripMargin

    val lines =
      input.split("\n").toList

    val actual = DocGenerator.extractComments(lines)

    val expected =
      List(
        """This is a comment
        |and it continues
        |across multiple lines""".stripMargin
      )

    assertEquals(actual, expected)
  }

  test("scaladoc style comment with markdown") {
    val input =
      """
      |val y = 2
      |/** [link](http://example.com)
      |  */
      |def foo: Unit =
      |  /** # Heading
      |    * This is a comment
      |    * and it continues
      |    * across multiple lines
      |    */
      |  val x = 1
      |  ()
      |""".stripMargin

    val lines =
      input.split("\n").toList

    val actual = DocGenerator.extractComments(lines)

    val expected =
      List(
        """[link](http://example.com)""",
        """# Heading
        |This is a comment
        |and it continues
        |across multiple lines
        |""".stripMargin.trim
      )

    assertEquals(actual, expected)
  }

  test("scaladoc style comment on a single line") {
    val input =
      """
      |/** Taking our 'pulse' boolean value, we can choose a range of 1 to 5, or 1 to 10. */
      |""".stripMargin.trim

    val lines =
      input.split("\n").toList

    val actual = DocGenerator.extractComments(lines)

    val expected =
      List(
        """Taking our 'pulse' boolean value, we can choose a range of 1 to 5, or 1 to 10."""
      )

    assertEquals(actual, expected)
  }

  test("code snippet no indent") {
    val input =
      """
      |val y = 2
      |// ```scala
      |val x = 1
      |//```
      |""".stripMargin

    val lines =
      input.split("\n").toList

    val actual = DocGenerator.extractComments(lines)

    val expected =
      List(
        """```scala
        |val x = 1
        |```""".stripMargin
      )

    assertEquals(actual, expected)
  }

  test("code snippet with indented code") {
    val input =
      """
      |val y = 2
      |/** Snippet example
      |  */
      |object Bar:
      |  // ```scala
      |  def foo: Unit =
      |    val x = 1
      |    ()
      |  // ```
      |""".stripMargin

    val lines =
      input.split("\n").toList

    val actual = DocGenerator.extractComments(lines)

    val expected =
      List(
        """Snippet example""",
        """
        |```scala
        |def foo: Unit =
        |  val x = 1
        |  ()
        |```
        |""".stripMargin.trim
      )

    assertEquals(actual, expected)
  }

  test(
    "code snippet with indented code at the end of a doc should unindent to the baseline correctly"
  ) {
    val input =
      """
      |
      |    // ```scala
      |    val clip: Clip[Material.Bitmap] =
      |      Clip(Size(64, 128), ClipSheet(9, FPS(10)), ClipPlayMode.default, Assets.assets.FlagMaterial)
      |
      |    Outcome(SceneUpdateFragment(clip))
      |    // ```
      |
      |""".stripMargin

    val lines =
      input.split("\n").toList

    val actual = DocGenerator.extractComments(lines)

    val expected =
      List(
        """```scala
        |val clip: Clip[Material.Bitmap] =
        |  Clip(Size(64, 128), ClipSheet(9, FPS(10)), ClipPlayMode.default, Assets.assets.FlagMaterial)
        |
        |Outcome(SceneUpdateFragment(clip))
        |```
        |""".stripMargin.trim
      )

    assertEquals(actual, expected)
  }

  test("Code snippet whitespace should be reduced to the minimum") {
    val input =
      """
      |    /** ## Encoding our animation as a `Signal`
      |      *
      |      * Let's encode our movement animation as a reusable, stateless signal.
      |      */
      |// ```scala
      |    def calculateXPosition(from: Int, to: Int, over: Seconds): Signal[Int] =
      |      Signal { t =>
      |        val maxDuration: Double     = over.toDouble
      |        val clampedTime: Double     = if (t.toDouble > maxDuration) maxDuration else t.toDouble
      |        val distanceToMove: Double  = to - from
      |        val pixelsPerSecond: Double = distanceToMove / maxDuration
      |  
      |        from + (pixelsPerSecond * clampedTime).toInt
      |      }
      |// ```
      |
      |""".stripMargin.trim

    val lines =
      input.split("\n").toList

    val actual = DocGenerator.extractComments(lines)

    val expected =
      List(
        """## Encoding our animation as a `Signal`
        |
        |Let's encode our movement animation as a reusable, stateless signal.
        |""".stripMargin.trim,
        """```scala
        |def calculateXPosition(from: Int, to: Int, over: Seconds): Signal[Int] =
        |  Signal { t =>
        |    val maxDuration: Double     = over.toDouble
        |    val clampedTime: Double     = if (t.toDouble > maxDuration) maxDuration else t.toDouble
        |    val distanceToMove: Double  = to - from
        |    val pixelsPerSecond: Double = distanceToMove / maxDuration
        |  
        |    from + (pixelsPerSecond * clampedTime).toInt
        |  }
        |```
        |""".stripMargin.trim
      )

    assertEquals(actual, expected)
  }

  test("code snippet with double indented code") {
    val input =
      """
      |val y = 2
      |/** Snippet example
      |  */
      |object Bar:
      |  object Baz:
      |    /** Testing
      |    */
      |    // ```scala
      |    def foo: Unit =
      |      val x = 1
      |      ()
      |    // ```
      |
      |""".stripMargin

    val lines =
      input.split("\n").toList

    val actual = DocGenerator.extractComments(lines)

    val expected =
      List(
        """Snippet example""",
        """Testing""",
        """
        |```scala
        |def foo: Unit =
        |  val x = 1
        |  ()
        |```
        |""".stripMargin.trim
      )

    assertEquals(actual, expected)
  }

  test("Can extract consecutive code snippets") {
    val input =
      """
      |val y = 2
      |/** First snippet example
      |  */
      |object Bar:
      |  // ```scala
      |  def firstMethod: Unit =
      |    val x = 1
      |    ()
      |  // ```
      |  // ```scala
      |  def secondMethod: Unit =
      |    val y = 2
      |    ()
      |  // ```
      |
      |""".stripMargin

    val lines =
      input.split("\n").toList

    val actual = DocGenerator.extractComments(lines)

    val expected =
      List(
        """First snippet example""",
        """
        |```scala
        |def firstMethod: Unit =
        |  val x = 1
        |  ()
        |```
        |""".stripMargin.trim,
        """
        |```scala
        |def secondMethod: Unit =
        |  val y = 2
        |  ()
        |```
        |""".stripMargin.trim
      )

    assertEquals(actual, expected)
  }

  test("Can extract text following a code snippet") {
    val input =
      """
      |val y = 2
      |/** Code snippet example
      |  */
      |object Bar:
      |  // ```scala
      |  def someMethod: Unit =
      |    val x = 1
      |    ()
      |  // ```
      |  /** This is explanatory text after the code snippet
      |    */
      |  val someValue = 42
      |
      |""".stripMargin

    val lines =
      input.split("\n").toList

    val actual = DocGenerator.extractComments(lines)

    val expected =
      List(
        """Code snippet example""",
        """
        |```scala
        |def someMethod: Unit =
        |  val x = 1
        |  ()
        |```
        """.stripMargin.trim,
        "This is explanatory text after the code snippet"
      )

    assertEquals(actual, expected)
  }

  test("compileMarkdown builds a complete markdown page from components") {
    val pageHeader = "# My Example Project\n\nThis is an example project.\n\n"

    val linksBlock =
      """## Example Links
      |
      |  - [View example code](https://github.com/example/repo/edit/main/src/example)
      |  - [Live demo](https://example.com/live_demos/example)
      |
      |""".stripMargin

    val comments = List(
      "This is a comment explaining the code",
      """```scala
      |def hello: Unit =
      |  println("Hello, World!")
      |```""".stripMargin,
      "This explains what happens after the code"
    )

    val actual = DocGenerator.compileMarkdown(pageHeader, linksBlock, comments)

    val expected =
      """# My Example Project
      |
      |This is an example project.
      |
      |## Example Links
      |
      |  - [View example code](https://github.com/example/repo/edit/main/src/example)
      |  - [Live demo](https://example.com/live_demos/example)
      |
      |This is a comment explaining the code
      |
      |```scala
      |def hello: Unit =
      |  println("Hello, World!")
      |```
      |
      |This explains what happens after the code""".stripMargin

    assertEquals(actual, expected)
  }
