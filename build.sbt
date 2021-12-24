import scala.language.reflectiveCalls
import scoverage.ScoverageKeys

Global / onChangedBuildSource := ReloadOnSourceChanges
Global / excludeLintKeys += scalacOptions

// Please use dodo to build the dependencies for the scrooge develop branch.  If
// you would like to instead do it manually, you need to publish util, and finagle locally:
// 'git checkout develop; sbt publishLocal' to publish SNAPSHOT versions of these projects.

// All Twitter library releases are date versioned as YY.MM.patch
val releaseVersion = "21.12.0"

lazy val versions = new {
  val slf4j = "1.7.30"
  val libthrift = "0.10.0"
}

def util(which: String) = "com.twitter" %% ("util-" + which) % releaseVersion
def finagle(which: String) = "com.twitter" %% ("finagle-" + which) % releaseVersion

val compileThrift = TaskKey[Seq[File]]("compile-thrift", "generate thrift needed for tests")

val dumpClasspath = TaskKey[File]("dump-classpath", "generate a file containing the full classpath")

val dumpClasspathSettings: Seq[Setting[_]] = Seq(
  dumpClasspath := {
    val base = baseDirectory.value
    val cp = (Runtime / fullClasspath).value
    val file = new File((base / ".classpath.txt").getAbsolutePath)
    val out = new java.io.FileWriter(file)
    try out.write(cp.files.absString)
    finally out.close()
    file
  }
)

val testThriftSettings: Seq[Setting[_]] = Seq(
  Test / sourceGenerators += ScroogeRunner.genTestThrift,
  ScroogeRunner.genTestThriftTask
)

val adaptiveScroogeTestThriftSettings = Seq(
  Test / sourceGenerators += ScroogeRunner.genAdaptiveScroogeTestThrift,
  ScroogeRunner.genAdaptiveScroogeTestThriftTask
)

def gcJavaOptions: Seq[String] = {
  val javaVersion = System.getProperty("java.version")
  if (javaVersion.startsWith("1.8")) {
    jdk8GcJavaOptions
  } else {
    jdk11GcJavaOptions
  }
}

def jdk8GcJavaOptions: Seq[String] = {
  Seq(
    "-XX:+UseParNewGC",
    "-XX:+UseConcMarkSweepGC",
    "-XX:+CMSParallelRemarkEnabled",
    "-XX:+CMSClassUnloadingEnabled",
    "-XX:ReservedCodeCacheSize=128m",
    "-XX:SurvivorRatio=128",
    "-XX:MaxTenuringThreshold=0",
    "-Xss8M",
    "-Xms512M",
    "-Xmx2G"
  )
}

def jdk11GcJavaOptions: Seq[String] = {
  Seq(
    "-XX:+UseConcMarkSweepGC",
    "-XX:+CMSParallelRemarkEnabled",
    "-XX:+CMSClassUnloadingEnabled",
    "-XX:ReservedCodeCacheSize=128m",
    "-XX:SurvivorRatio=128",
    "-XX:MaxTenuringThreshold=0",
    "-Xss8M",
    "-Xms512M",
    "-Xmx2G"
  )
}

def travisTestJavaOptions: Seq[String] = {
  // We have some custom configuration for the Travis environment
  // https://docs.travis-ci.com/user/environment-variables/#default-environment-variables
  val travisBuild = sys.env.getOrElse("TRAVIS", "false").toBoolean
  if (travisBuild) {
    Seq(
      "-DSKIP_FLAKY=true",
      "-DSKIP_FLAKY_TRAVIS=true"
    )
  } else {
    Seq(
      "-DSKIP_FLAKY=true"
    )
  }
}

// scalac options for projects that are scala 2.10
// or cross compiled with scala 2.10
val scalacTwoTenOptions =
Seq("-deprecation", "-unchecked", "-feature", "-Xlint", "-encoding", "utf8")

val sharedSettingsWithoutScalaVersion = Seq(
  version := "21.8.1-SNAPSHOT",
  organization := "com.tundra",
  libraryDependencies ++= Seq(
    "junit" % "junit" % "4.12" % "test",
    "org.scalatest" %% "scalatest" % "3.1.2" % "test",
    "org.scalatestplus" %% "junit-4-12" % "3.1.2.0" % "test",
    "org.scalatestplus" %% "mockito-1-10" % "3.1.0.0" % "test",
    "org.scalatestplus" %% "scalacheck-1-14" % "3.1.2.0" % "test"
  ),
  ScoverageKeys.coverageHighlighting := true,
  Test / fork := true, // We have to fork to get the JavaOptions
  Test / parallelExecution := false,
  javaOptions ++= Seq(
    "-Djava.net.preferIPv4Stack=true",
    "-XX:+AggressiveOpts",
    "-server"
  ),
  javaOptions ++= gcJavaOptions,
  Test / javaOptions ++= travisTestJavaOptions,
  // -a: print stack traces for failing asserts
  testOptions += Tests.Argument(TestFrameworks.JUnit, "-a"),
  // Sonatype publishing
  Test / publishArtifact := false,
  pomIncludeRepository := { _ => false },
  publishMavenStyle := true,
  publishConfiguration := publishConfiguration.value.withOverwrite(true),
  publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true),

  packageDoc / publishArtifact  := false,
  packageSrc /publishArtifact := false,
  Test / publishArtifact := false,
  publishTo := {
    if (isSnapshot.value)
      Some("snapshots" at "https://nexus.tundra-shared.com/repository/maven-snapshots/")
    else
      Some("releases" at "https://nexus.tundra-shared.com/repository/maven-releases/")
  },
  credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
  resolvers :=
    Seq(
      "Tundra Nexus" at "https://nexus.tundra-shared.com/repository/tundra/",
      "Tundra releases" at "https://nexus.tundra-shared.com/repository/maven-releases/",
      "SBT release" at "https://dl.bintray.com/sbt/sbt-plugin-releases/",
      "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/",
      "sonatype-public" at "https://oss.sonatype.org/content/groups/public"
    ),

  pomExtra :=
    <url>https://github.com/tundra/scrooge</url>
    <licenses>
      <license>
        <name>Apache License, Version 2.0</name>
        <url>https://www.apache.org/licenses/LICENSE-2.0</url>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:tundra/scrooge.git</url>
      <connection>scm:git:git@github.com:tundra/scrooge.git</connection>
    </scm>
    <developers>
      <developer>
        <id>twitter</id>
        <name>Twitter Inc.</name>
        <url>https://www.twitter.com/</url>
      </developer>
      <developer>
        <id>tundra</id>
        <name>Tundra Inc.</name>
        <url>https://www.tundra.com/</url>
      </developer>
    </developers>,
  Compile / resourceGenerators += Def.task {
    val dir = (Compile / resourceManaged).value
    val file = dir / "com" / "twitter" / name.value / "build.properties"
    val buildRev = scala.sys.process.Process("git" :: "rev-parse" :: "HEAD" :: Nil).!!.trim
    val buildName = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss").format(new java.util.Date)
    val contents =
      s"name=${name.value}\nversion=${version.value}\nbuild_revision=$buildRev\nbuild_name=$buildName"
    IO.write(file, contents)
    Seq(file)
  }
)

// settings for projects that are scala 2.10
val settingsWithTwoTen =
  sharedSettingsWithoutScalaVersion ++
    Seq(
      scalaVersion := "2.10.7",
      scalacOptions := scalacTwoTenOptions,
      javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint:unchecked"),
      doc / javacOptions := Seq("-source", "1.8"),
      libraryDependencies ++= Seq(
        "org.scalacheck" %% "scalacheck" % "1.14.3" % "test"
      )
    )

// settings for projects that are cross compiled with scala 2.10
val settingsCrossCompiledWithTwoTen =
  sharedSettingsWithoutScalaVersion ++
    Seq(
      crossScalaVersions := Seq("2.10.7", "2.12.12", "2.13.1"),
      scalaVersion := "2.13.6",
      scalacOptions := scalacTwoTenOptions,
      javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint:unchecked"),
      doc / javacOptions := Seq("-source", "1.8"),
      libraryDependencies ++= Seq(
        "org.scalacheck" %% "scalacheck" % "1.14.3" % "test"
      )
    )

val sharedSettings =
  sharedSettingsWithoutScalaVersion ++
    Seq(
      scalaVersion := "2.13.6",
      crossScalaVersions := Seq("2.12.8", "2.13.6"),
      scalacOptions := Seq(
        "-deprecation",
        "-unchecked",
        "-feature",
        "-Xlint",
        "-encoding",
        "utf8",
        "-target:jvm-1.8",
        "-Ypatmat-exhaust-depth",
        "40"
      ),
      javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint:unchecked"),
      doc / javacOptions := Seq("-source", "1.8"),
      libraryDependencies ++= Seq(
        "org.scalacheck" %% "scalacheck" % "1.15.4" % "test"
      )
    )

val jmockSettings = Seq(
  libraryDependencies ++= Seq(
    "cglib" % "cglib" % "3.2.8" % "test",
    "org.jmock" % "jmock" % "2.9.0" % "test",
    "org.jmock" % "jmock-legacy" % "2.9.0" % "test",
    "org.mockito" % "mockito-core" % "1.9.5" % "test",
    "org.objenesis" % "objenesis" % "1.1" % "test",
    "org.ow2.asm" % "asm" % "6.2.1" % "test",
    "org.scalatestplus" %% "jmock-2-8" % "3.1.2.0" % "test"
  )
)

// this omits scrooge-generator, since it needs special treatment for 2.10.x
lazy val publishedProjects =
  Seq[sbt.ProjectReference](scroogeAdaptive, scroogeCore, scroogeLinter, scroogeSerializer)

lazy val scrooge = Project(
  id = "scrooge",
  base = file(".")
).enablePlugins(
  ScalaUnidocPlugin
).settings(
  sharedSettings
).aggregate(publishedProjects: _*)

// This target is used for publishing dependencies locally
// and is used for generating all(*) of the dependencies
// needed for Finagle, including cross Scala version support.
//
// (*) Unfortunately, sbt plugins are currently only supported
// with Scala 2.10 and as such we cannot include that project
// here and it should be published separately to Scala 2.10.
lazy val scroogePublishLocal = Project(
  id = "scrooge-publish-local",
  // use a different target so that we don't have conflicting output paths
  // between this and the `scrooge` target.
  base = file("target/")
).settings(
  sharedSettings
).aggregate(publishedProjects: _*)

// must be cross compiled with scala 2.10 because scrooge-sbt-plugin
// has a dependency on this.
lazy val scroogeGenerator = Project(
  id = "scrooge-generator",
  base = file("scrooge-generator")
).settings(
  settingsCrossCompiledWithTwoTen
).settings(
  name := "scrooge-generator",
  scalaVersion := "2.12.12",libraryDependencies ++= Seq(
    "org.apache.thrift" % "libthrift" % versions.libthrift,
    "com.github.scopt" %% "scopt" % "4.0.0-RC2",
    "com.github.spullara.mustache.java" % "compiler" % "0.8.18",
    "org.codehaus.plexus" % "plexus-utils" % "1.5.4",
    "com.google.code.findbugs" % "jsr305" % "2.0.1",
    "commons-cli" % "commons-cli" % "1.3.1"
  ).++(CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, x)) if x >= 11 =>
      Seq("org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2")
    case _ => Nil
  }),
  assembly / test := {}, // Skip tests when running assembly.
  assembly / mainClass := Some("com.twitter.scrooge.Main")
)

lazy val scroogeGeneratorTests = Project(
  id = "scrooge-generator-tests",
  base = file("scrooge-generator-tests")
).settings(
  inConfig(Test)(testThriftSettings),
  sharedSettings,
  jmockSettings
).settings(
  name := "scrooge-generator-tests",
  libraryDependencies ++= Seq(
    "com.novocode" % "junit-interface" % "0.8" % "test->default" exclude ("org.mockito", "mockito-all"),
    "org.slf4j" % "slf4j-nop" % versions.slf4j % "test", // used in thrift transports
    finagle("thrift") % "test",
    finagle("thriftmux") % "test"
  ),
  assembly / test := {}, // Skip tests when running assembly.
  publishArtifact := false
).dependsOn(scroogeCore, scroogeGenerator)

lazy val scroogeCore = Project(
  id = "scrooge-core",
  base = file("scrooge-core")
).settings(
  sharedSettings
).settings(
  name := "scrooge-core",
  libraryDependencies ++= Seq(
    "org.apache.thrift" % "libthrift" % versions.libthrift % "provided",
    "javax.annotation" % "javax.annotation-api" % "1.3.2",
    util("core"),
    util("validator")
  )
)

val serializerTestThriftSettings: Seq[Setting[_]] = Seq(
  Test / sourceGenerators += ScroogeRunner.genSerializerTestThrift,
  ScroogeRunner.genSerializerTestThriftTask
)

lazy val scroogeSerializer = Project(
  id = "scrooge-serializer",
  base = file("scrooge-serializer")
).settings(
  inConfig(Test)(serializerTestThriftSettings),
  sharedSettings
).settings(
  name := "scrooge-serializer",
  libraryDependencies ++= Seq(
    util("app"),
    util("codec"),
    "org.slf4j" % "slf4j-nop" % versions.slf4j % "test",
    "org.apache.thrift" % "libthrift" % versions.libthrift % "provided"
  )
).dependsOn(scroogeCore, scroogeGenerator % "test")

lazy val scroogeAdaptive = Project(
  id = "scrooge-adaptive",
  base = file("scrooge-adaptive")
).settings(
  inConfig(Test)(adaptiveScroogeTestThriftSettings),
  sharedSettings
).settings(
  name := "scrooge-adaptive",
  libraryDependencies ++= Seq(
    "org.ow2.asm" % "asm" % "6.2.1",
    "org.ow2.asm" % "asm-commons" % "6.2.1",
    "org.ow2.asm" % "asm-util" % "6.2.1",
    "org.apache.thrift" % "libthrift" % versions.libthrift % "provided",
    util("logging")
  )
).dependsOn(scroogeCore, scroogeGenerator % "test", scroogeSerializer)

lazy val scroogeSbtPlugin = Project(
  id = "scrooge-sbt-plugin",
  base = file("scrooge-sbt-plugin")
).enablePlugins(BuildInfoPlugin).settings(
  settingsWithTwoTen: _*
).settings(
  scalaVersion := "2.12.12",
  crossSbtVersions := Seq("0.13.18", "1.5.3"),
  buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
  buildInfoPackage := "com.twitter",
  sbtPlugin := true,
).dependsOn(scroogeGenerator)

lazy val scroogeLinter = Project(
  id = "scrooge-linter",
  base = file("scrooge-linter")
).settings(
  sharedSettings
).settings(
  name := "scrooge-linter",
  libraryDependencies ++= Seq(
    util("logging"),
    util("app")
  )
).dependsOn(scroogeGenerator)

val benchThriftSettings: Seq[Setting[_]] = Seq(
  Compile / sourceGenerators += ScroogeRunner.genBenchmarkThrift,
  ScroogeRunner.genBenchmarkThriftTask
)

lazy val scroogeBenchmark = Project(
  id = "scrooge-benchmark",
  base = file("scrooge-benchmark")
).settings(
  inConfig(Compile)(benchThriftSettings),
  sharedSettings,
  dumpClasspathSettings
).enablePlugins(
  JmhPlugin
).settings(
  libraryDependencies ++= Seq(
    "org.slf4j" % "slf4j-nop" % versions.slf4j, // Needed for the thrift transports
    "org.apache.thrift" % "libthrift" % versions.libthrift
  )
).dependsOn(
  scroogeAdaptive % "compile->test", // Need ReloadOnceAdaptBinarySerializer defined in test
  scroogeGenerator,
  scroogeSerializer
)

lazy val scroogeDoc = Project(
  id = "scrooge-doc",
  base = file("doc")
).enablePlugins(
  SphinxPlugin
).settings(
  sharedSettings,
  Seq(
    doc / scalacOptions ++= Seq("-doc-title", "Scrooge", "-doc-version", version.value),
    Sphinx / includeFilter := ("*.html" | "*.png" | "*.js" | "*.css" | "*.gif" | "*.txt")
  )
).configs(DocTest).settings(
  inConfig(DocTest)(Defaults.testSettings): _*
).settings(
  DocTest / unmanagedSourceDirectories += baseDirectory.value / "src/sphinx/code",
  // Make the "test" command run both, test and doctest:test
  test := Seq(Test / test, DocTest / test).dependOn.value
)

/* Test Configuration for running tests on doc sources */
lazy val DocTest = config("testdoc") extend Test
