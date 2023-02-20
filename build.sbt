ThisBuild / organization := "org.apatheia"
ThisBuild / scalaVersion := "2.13.5"
ThisBuild / version := "0.0.1-alpha"

lazy val root = (project in file(".")).settings(
  name := "apatheia-network",
  libraryDependencies ++= Seq(
    // "core" module - IO, IOApp, schedulers
    // This pulls in the kernel and std modules automatically.
    "org.typelevel" %% "cats-effect" % "3.3.12",
    // concurrency abstractions and primitives (Concurrent, Sync, Async etc.)
    "org.typelevel" %% "cats-effect-kernel" % "3.3.12",
    // standard "effect" library (Queues, Console, Random etc.)
    "org.typelevel" %% "cats-effect-std" % "3.3.12",

    // apatheia protocol
    "org.apatheia" %% "apatheia-p2p-protocol" % "0.0.1-alpha",

    // better monadic for compiler plugin as suggested by documentation
    compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    "org.scalactic" %% "scalactic" % "3.2.15",
    "org.scalatest" %% "scalatest" % "3.2.15" % "test",
    "org.scalatestplus" %% "mockito-4-6" % "3.2.15.0" % "test",
    "org.mockito" % "mockito-core" % "4.6.0" % "test"
  )
)

// Resolver config
resolvers ++= Seq(
  "Apatheia P2P Repository Manager" at "https://maven.pkg.github.com/apatheia-org/apatheia-p2p-network"
)

// publish to github packages settings
ThisBuild / publishTo := Some(
  "GitHub Apatheia's Apache Maven Packages" at "https://maven.pkg.github.com/apatheia-org/apatheia-network"
)
ThisBuild / publishMavenStyle := true
ThisBuild / credentials += Credentials(
  "GitHub Package Registry",
  "maven.pkg.github.com",
  "adrianobrito",
  System.getenv("GITHUB_TOKEN")
)