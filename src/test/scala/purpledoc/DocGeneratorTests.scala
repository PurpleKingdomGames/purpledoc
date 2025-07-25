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
