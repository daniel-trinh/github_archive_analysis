package object PreRun {
  val imports = """
                  |import org.joda.time.DateTime
                  |import com.typesafe.config._
                  |import org.joda.time.DateTime
                  |import scala.util.{Success, Failure, Try}
                  |import dispatch._
                  |import dispatch.Defaults._
                  |import org.json4s._
                  |import org.json4s.jackson.JsonMethods._
                  |import org.json4s.jackson.Serialization.{read, write, writePretty}
                  |import scalax.io._
                  |import githubarchive._
                  |
                  |import org.apache.hadoop.conf.Configuration
                  |import org.apache.hadoop.fs.{Path, FileSystem}
                  |
                  |// Macro bullshit
                  |val universe: scala.reflect.runtime.universe.type = scala.reflect.runtime.universe
                  |import universe._
                  |import scala.reflect.runtime.currentMirror
                  |import scala.tools.reflect.ToolBox
                  |val toolbox = currentMirror.mkToolBox()
                  |""".stripMargin
  val commands = """
                   |new play.core.StaticApplication(new java.io.File("."))
                   |implicit val format = org.json4s.DefaultFormats
                 """.stripMargin
}