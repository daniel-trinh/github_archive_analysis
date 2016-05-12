import sbt._
import sbt.Keys._
import sbtassembly.{AssemblyKeys, PathList, MergeStrategy}
import sbtdatabricks.DatabricksPlugin.autoImport._
import AssemblyKeys._

object GithubAnalysisBuild extends Build {

  val scalaCompilerVersion = "2.11.7"

  val baseDependencies = Seq(
    "com.danieltrinh" %% "utils" % "0.2.0"
  )
  val meta = """META.INF(.)*""".r

  lazy val jobs = Project(
    "jobs", file("jobs")
  ).settings(
      scalaVersion := scalaCompilerVersion,
      dbcUsername := "daniel.s.trinh@gmail.com",
      dbcPassword := "8YbwnIgEftWt",
      dbcLibraryPath := "/Users/dtrinh/src/oss/go/src/github.com/daniel-trinh/github_archive/lib/",
      dbcApiUrl := "https://community.cloud.databricks.com/api/1.2",
      dbcClusters += "ALL_CLUSTERS",
      dbcRestartOnAttach := false,
      libraryDependencies ++= Seq(
        "org.json4s" %% "json4s-jackson" % "3.3.0",
        "com.github.nscala-time" %% "nscala-time" % "2.12.0",
        "org.apache.spark" %% "spark-sql" % "1.6.1" % "provided",
        "org.apache.spark" %% "spark-core" % "1.6.1" % "provided",
        "org.apache.hadoop" % "hadoop-aws" % "2.6.0",
        "com.databricks" % "spark-redshift_2.11" % "0.6.0"
      ),
      assemblyMergeStrategy in assembly := {
      case PathList("org", "apache", xs @ _*) => MergeStrategy.first
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    }
  )
}