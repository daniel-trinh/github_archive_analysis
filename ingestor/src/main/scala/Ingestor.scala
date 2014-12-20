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

case class IngestorConfig(daysAgo: Int = 1, dateToIngest: DateTime = DateTime.now(UTC))

object Ingestor extends App {
  val parser = new scopt.OptionParser[IngestorConfig]("github_ingestor") {
    head("github_ingestor", "3.x")
    opt[Int]('a', "daysAgo") optional() action { (x, c) =>
      c.copy(daysAgo = x) } text("How many days ago from 'dateToIngest' to pull data from")
    opt[String]('d', "dateToIngest") optional() action { (x, c) =>
      c.copy(dateToIngest = DateTime.parse(x)) } text(
        """
          |The day of reference to pull data from.
          |
          |Must be in format "yyyy-MM-dd"
          |If dateToIngest is today, and daysAgo is 1, this will ingest data from yesterday.
          |If dateToIngest is 2014-06-02, and daysAgo is 0, this will ingest data from 2014-06-02.
        """.stripMargin
      )
  }

  parser.parse(args, IngestorConfig()) match {
    case Some(config) =>
      val dayToPull = config.dateToIngest.minusDays(config.daysAgo)
      val logger = Logger(LoggerFactory.getLogger(this.getClass))
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
    case None =>
      sys.exit(1)
  }
}