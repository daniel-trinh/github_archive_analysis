package ingestor

import com.typesafe.scalalogging.Logger
import org.joda.time.DateTime
import org.joda.time.DateTimeZone._
import GithubArchiveIngestor._
import stores._
import scala.concurrent.ExecutionContext.Implicits.global
import com.danieltrinh.FutureExtensions._
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.{Try, Success, Failure}
import spray.caching.SimpleLruCache
import org.slf4j.LoggerFactory
import scala.sys.process._

case class IngestorConfig(daysAgo: Option[Int] = None, dateToIngest: Option[DateTime] = None)

object Ingestor extends App {
  val parser = new scopt.OptionParser[IngestorConfig]("github_ingestor") {
    head("github_ingestor", "3.x")
    opt[Int]('a', "daysAgo") optional() action { (x, c) =>
      c.copy(daysAgo = Some(x)) } text
      "How many days ago from 'dateToIngest' to pull data from"
    opt[String]('d', "dateToIngest") optional() action { (x, c) =>
      c.copy(dateToIngest = Some(DateTime.parse(x))) } text
      "The day of reference to pull data from. Overrides daysAgo if present."
  }
  val logger = Logger(LoggerFactory.getLogger(this.getClass))

  parser.parse(args, IngestorConfig()) match {
    case Some(config) =>
      val IngestorConfig(daysAgoOpt, dateToIngestOpt) = config

      val dayToPull = dateToIngestOpt.getOrElse {
        val daysAgo = daysAgoOpt.getOrElse(2)
        DateTime.now(UTC).minusDays(daysAgo)
      }

      val hourlyDateTimes = oneDayOfHours(dayToPull)
      //  implicit val store = InMemoryStore(new SimpleLruCache[String](10, 10))
      implicit val store = HdfsStore()

      val attempts = hourlyDateTimes.map { time =>
        logger.info(s"Pulling data for $time")
        fetchAndWriteData(time)
      }

      if (attempts.exists(_.isFailure)) {
        val errors = attempts.filter(_.isFailure).flatMap(_.failed.toOption)
        errors.foreach { error =>
          logger.error(error.getMessage)
          logger.error(error.getStackTrace.mkString("\n"))
        }
        sys.exit(1)
      } else {
        logger.info(s"Successfully pulled data for $dayToPull")
        sys.exit(0)
      }
    case None =>
      sys.exit(1)
  }

  def fetchAndWriteData(time: DateTime): Try[Unit] = {
    val filepath = s"${HourlyData.dateSuffix(time)}.json"
    val url = GithubArchiveIngestor.endpoint(time)

    // Just in case a previous job failed.
    val cleanup = s"rm $filepath" !

    val status = s"wget $url" #&&
      s"gunzip $filepath.gz" #&&
      s"hdfs dfs -moveFromLocal $filepath /${time.toString("yyyy-MM-dd")}/${time.hourOfDay().get()}.json" !

    if (status != 0) {
      val errorMsg = s"Failed to fetch and store data for $time"
      Failure(new RuntimeException(errorMsg))
    } else {
      Success(())
    }
  }
}