package ingestor

import org.joda.time.DateTime
import org.joda.time.DateTimeZone._
import GithubArchiveIngestor._
import stores._
import scala.concurrent.ExecutionContext.Implicits.global
import com.danieltrinh.FutureExtensions._
import scala.concurrent.Future
import scala.util.{Success, Failure}
import spray.caching.SimpleLruCache

object Ingestor extends App {

  val yesterday = DateTime.now(UTC).minusDays(1)
  val hourlyDateTimes = oneDayOfHours(yesterday)
//  implicit val store = InMemoryStore(new SimpleLruCache[String](10, 10))
  implicit val store = HdfsStore()
  val attempt = Future.serialiseFutures(hourlyDateTimes) { time =>
    println(s"Pulling data for $time")
    pullAndWrite(time)
  }

  attempt.onComplete {
    case Success(_) =>
      sys.exit(0)
    case Failure(e) =>
      println(e)
      sys.exit(1)
  }
}
