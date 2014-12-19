package ingestor

import com.typesafe.scalalogging.Logger
import org.joda.time.DateTime
import org.joda.time.DateTimeZone._
import GithubArchiveIngestor._
import stores._
import scala.concurrent.ExecutionContext.Implicits.global
import com.danieltrinh.FutureExtensions._
import scala.concurrent.Future
import scala.util.{Try, Success, Failure}
import spray.caching.SimpleLruCache
import org.slf4j.LoggerFactory

object Ingestor extends App {
  val daysAgo = Try(args(0)).getOrElse("1").toInt

  val logger = Logger(LoggerFactory.getLogger("github_ingestor"))

  val dayToPull = DateTime.now(UTC).minusDays(daysAgo)
  val hourlyDateTimes = oneDayOfHours(dayToPull)
//  implicit val store = InMemoryStore(new SimpleLruCache[String](10, 10))
  implicit val store = HdfsStore()
  val attempt = Future.serialiseFutures(hourlyDateTimes) { time =>
    logger.info(s"Pulling data for $time")
    pullAndWrite(time)
  }
  attempt.onComplete {
    case Success(_) =>
      sys.exit(0)
    case Failure(e) =>
      logger.error(e.getMessage)
      logger.error(e.getStackTrace.mkString("\n"))
      sys.exit(1)
  }
}