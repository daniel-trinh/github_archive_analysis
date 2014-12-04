import org.joda.time.DateTime
import com.typesafe.config._
import org.joda.time.DateTime
import scala.util.{Success, Failure, Try}
import dispatch._
import dispatch.Defaults._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization.{read, write, writePretty}
import scalax.io._
import githubarchive._

GithubArchiveIngestor.pullData(DateTime.now.minusDays(1)).apply()