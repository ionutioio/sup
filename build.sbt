val Scala_213 = "2.13.0"
val Scala_212 = "2.12.8"

val catsEffectVersion          = "2.0.0-RC1"
val catsTaglessVersion         = "0.9"
val catsParVersion             = "1.0.0-RC1"
val doobieVersion              = "0.8.0-M1"
val catsVersion                = "2.0.0-RC1"
val scalacheckShapelessVersion = "1.2.3"
val scalatestVersion           = "3.0.8"
val simulacrumVersion          = "0.19.0"
val scalacacheVersion          = "0.28.0"
val kindProjectorVersion       = "0.10.3"
val refinedVersion             = "0.9.8"
val fs2RedisVersion            = "0.8.3"
val h2Version                  = "1.4.199"
val log4CatsVersion            = "0.4.0-M1"
val http4sVersion              = "0.21.0-M3"
val circeVersion               = "0.12.0-RC2"
val sttpVersion                = "1.6.4"

inThisBuild(
  List(
    organization := "com.kubukoz",
    homepage := Some(url("https://github.com/kubukoz/sup")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(
      Developer(
        "kubukoz",
        "Jakub Kozłowski",
        "kubukoz@gmail.com",
        url("https://kubukoz.com")
      )
    )
  )
)

val compilerPlugins = List(
  compilerPlugin("org.typelevel" %% "kind-projector" % kindProjectorVersion)
)

val commonSettings = Seq(
  scalaVersion := Scala_212,
  scalacOptions ++= Options.all(scalaVersion.value),
  fork in Test := true,
  name := "sup",
  updateOptions := updateOptions.value.withGigahorse(false),
  libraryDependencies ++= Seq(
    "org.typelevel"              %% "cats-tagless-laws"         % catsTaglessVersion         % Test,
    "org.typelevel"              %% "cats-effect-laws"          % catsEffectVersion          % Test,
    "org.typelevel"              %% "cats-testkit"              % catsVersion                % Test,
    "org.typelevel"              %% "cats-laws"                 % catsVersion                % Test,
    "org.typelevel"              %% "cats-kernel-laws"          % catsVersion                % Test,
    "com.github.alexarchambault" %% "scalacheck-shapeless_1.14" % scalacheckShapelessVersion % Test,
    "org.scalatest"              %% "scalatest"                 % scalatestVersion           % Test
  ) ++ compilerPlugins,
  mimaPreviousArtifacts := (if (under213(scalaVersion.value))
                              Set(organization.value %% name.value.toLowerCase % "0.2.0")
                            else Set.empty)
)

def under213(scalaVersion: String): Boolean = scalaVersion != Scala_213

val crossBuiltCommonSettings = commonSettings ++ Seq(crossScalaVersions := Seq(Scala_212, Scala_213))

def module(moduleName: String): Project =
  Project(moduleName, file("modules/" + moduleName))
    .settings(crossBuiltCommonSettings)
    .settings(name += s"-$moduleName")

val core = module("core").settings(
  libraryDependencies ++= Seq(
    "com.github.mpilquist" %% "simulacrum"        % simulacrumVersion,
    "org.typelevel"        %% "cats-effect"       % catsEffectVersion,
    "io.chrisdavenport"    %% "cats-par"          % catsParVersion,
    "org.typelevel"        %% "cats-tagless-core" % catsTaglessVersion
  )
)

val scalacache = module("scalacache")
  .settings(
    libraryDependencies ++= Seq(
      "com.github.cb372" %% "scalacache-core" % scalacacheVersion
    )
  )
  .dependsOn(core)

val doobie = module("doobie")
  .settings(
    libraryDependencies ++= Seq(
      "org.tpolecat"   %% "doobie-core" % doobieVersion,
      "eu.timepit"     %% "refined"     % refinedVersion,
      "com.h2database" % "h2"           % h2Version % Test
    )
  )
  .dependsOn(core % "compile->compile;test->test")

val redis = module("redis")
  .settings(
    scalaVersion := Scala_212,
    crossScalaVersions := List(Scala_212, Scala_213),
    libraryDependencies ++= Seq(
      "dev.profunktor" %% "redis4cats-effects" % fs2RedisVersion
    )
  )
  .dependsOn(core)

val log4cats = module("log4cats")
  .settings(
    libraryDependencies ++= Seq(
      "io.chrisdavenport" %% "log4cats-core" % log4CatsVersion
    )
  )
  .dependsOn(core)

val http4s = module("http4s")
  .settings(
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-core" % http4sVersion,
      "org.http4s" %% "http4s-dsl"  % http4sVersion
    )
  )
  .dependsOn(core)

val http4sClient = module("http4s-client")
  .settings(
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-client" % http4sVersion
    )
  )
  .dependsOn(core)

val circe = module("circe")
  .settings(
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-generic" % circeVersion
    )
  )
  .dependsOn(core)

val sttp = module("sttp")
  .settings(
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp" %% "core" % sttpVersion
    )
  )
  .dependsOn(core)

val allModules = List(core /*,scalacache, doobie, redis, log4cats, http4s, http4sClient, circe, sttp*/)

val microsite = project
  .settings(
    crossScalaVersions := List(),
    micrositeName := "sup",
    micrositeDescription := "Functional healthchecks in Scala",
    micrositeUrl := "https://sup.kubukoz.com",
    micrositeAuthor := "Jakub Kozłowski",
    micrositeTwitterCreator := "@kubukoz",
    micrositeGithubOwner := "kubukoz",
    micrositeGithubRepo := "sup",
    micrositeGitterChannel := false,
    micrositePushSiteWith := GitHub4s,
    micrositeGithubToken := sys.env.get("GITHUB_TOKEN"),
    //doesn't fork anyway though
    fork in makeMicrosite := true,
    scalacOptions ++= Options.all(scalaVersion.value),
    scalacOptions --= Seq("-Ywarn-unused:imports"),
    libraryDependencies ++= compilerPlugins,
    libraryDependencies ++= Seq(
      "io.chrisdavenport" %% "log4cats-core" % log4CatsVersion,
      "org.http4s"        %% "http4s-circe"    % http4sVersion
    ),
    skip in publish := true,
    buildInfoPackage := "sup.buildinfo",
    micrositeAnalyticsToken := "UA-55943015-9",
    buildInfoKeys := Seq[BuildInfoKey](version)
  )
  .enablePlugins(MicrositesPlugin)
  .dependsOn(allModules.map(x => x: ClasspathDep[ProjectReference]): _*)
  .enablePlugins(BuildInfoPlugin)

val sup =
  project
    .in(file("."))
    .settings(commonSettings)
    .settings(publishArtifact := false, crossScalaVersions := List(), mimaPreviousArtifacts := Set.empty)
    .aggregate((microsite :: allModules).map(x => x: ProjectReference): _*)
