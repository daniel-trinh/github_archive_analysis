import play.PlayImport.PlayKeys._

val originalJvmOptions = sys.process.javaVmArguments.filter(
  a => Seq("-Xmx", "-Xms", "-XX").exists(a.startsWith)
)

val baseSettings = Seq(
  scalaVersion := "2.11.2",
  scalacOptions := Seq("-language:_", "-deprecation", "-unchecked", "-Xlint"),
  watchSources ~= { _.filterNot(f => f.getName.endsWith(".swp") || f.getName.endsWith(".swo") || f.isDirectory) },
  javaOptions ++= originalJvmOptions,
  routesImport += "binders.QueryBinders._",
  initialCommands += PreRun.imports + PreRun.commands,
  shellPrompt := { state =>
    val branch = if(file(".git").exists){
      "git branch".lines_!.find{_.head == '*'}.map{_.drop(1)}.getOrElse("")
    }else ""
    Project.extract(state).currentRef.project + branch + " > "
  },
  incOptions := incOptions.value.withNameHashing(true),
  resolvers ++= Seq(Opts.resolver.sonatypeReleases)
)

lazy val root = Project(
  "github_archiver", file(".")
).enablePlugins(PlayScala).settings(
  baseSettings: _*
).settings(
  libraryDependencies ++= Seq(
  )
)

resolvers += "rediscala" at "http://dl.bintray.com/etaty/maven"

libraryDependencies ++= Seq(
  filters,
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.2",
  "com.sksamuel.elastic4s" %% "elastic4s" % "1.2.3.0",
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.1",
  "com.github.mauricio" %% "postgresql-async" % "0.2.15",
  "com.etaty.rediscala" %% "rediscala" % "1.4.0",
  "org.json4s" %% "json4s-jackson" % "3.2.10",
  "com.github.scala-incubator.io" %% "scala-io-core" % "0.4.3",
  "com.github.scala-incubator.io" %% "scala-io-file" % "0.4.3",
  "com.github.nscala-time" %% "nscala-time" % "1.4.0",
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "org.apache.hadoop" % "hadoop-client" % "2.5.2",
  "io.spray" %% "spray-caching" % "1.3.2"
)