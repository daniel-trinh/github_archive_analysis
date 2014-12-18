import play.PlayImport.PlayKeys._
import play.Play.autoImport._
import play.PlayScala
import sbt._
import sbt.Keys._
import bintray.Plugin._

object GithubAnalysisBuild extends Build {

  val scalaCompilerVersion = "2.11.4"

  val baseDependencies = Seq(
    "com.github.nscala-time" %% "nscala-time" % "1.4.0",
    "org.scala-lang" % "scala-reflect" % scalaCompilerVersion,
    "org.json4s" %% "json4s-jackson" % "3.2.10",
    "net.databinder.dispatch" %% "dispatch-core" % "0.11.2",
    "com.github.scala-incubator.io" %% "scala-io-core" % "0.4.3",
    "com.github.scala-incubator.io" %% "scala-io-file" % "0.4.3",
    "com.github.nscala-time" %% "nscala-time" % "1.4.0",
    "org.scala-lang" % "scala-reflect" % scalaCompilerVersion,
    "com.danieltrinh" %% "utils" % "0.2.0"
  )

  lazy val webapp = Project(
    "webapp", file(".")
  ).enablePlugins(PlayScala).settings(
    baseSettings: _*
  ).settings(
    libraryDependencies ++= Seq(
      filters,
      "com.sksamuel.elastic4s" %% "elastic4s" % "1.2.3.0",
      "com.github.mauricio" %% "postgresql-async" % "0.2.15",
      "com.etaty.rediscala" %% "rediscala" % "1.4.0"
    ),
    routesImport += "binders.QueryBinders._"
  ).aggregate(injestor)

  lazy val injestor = Project(
    "ingestor", file("ingestor")
  ).settings(
    baseSettings: _*
  ).settings(
      fork in run := true,
      libraryDependencies ++= Seq(
      "org.apache.hadoop" % "hadoop-client" % "2.5.2",
      "io.spray" %% "spray-caching" % "1.3.2"
    )
  )

  val originalJvmOptions = sys.process.javaVmArguments.filter(
    a => Seq("-Xmx", "-Xms", "-XX").exists(a.startsWith)
  )

  val baseSettings = Seq(
    scalaVersion := scalaCompilerVersion,
    scalacOptions := Seq("-language:_", "-deprecation", "-unchecked", "-Xlint"),
    watchSources ~= { _.filterNot(f => f.getName.endsWith(".swp") || f.getName.endsWith(".swo") || f.isDirectory) },
    javaOptions ++= originalJvmOptions,
    initialCommands += PreRun.imports + PreRun.commands,
    shellPrompt := { state =>
      val branch = if(file(".git").exists){
        "git branch".lines_!.find{_.head == '*'}.map{_.drop(1)}.getOrElse("")
      }else ""
      Project.extract(state).currentRef.project + branch + " > "
    },
    incOptions := incOptions.value.withNameHashing(true),
    resolvers ++= Seq(
      Opts.resolver.sonatypeReleases,
      "utils" at "http://dl.bintray.com/daniel-trinh/maven",
      "rediscala" at "http://dl.bintray.com/etaty/maven"
    ),
    libraryDependencies ++= baseDependencies
  )
}

