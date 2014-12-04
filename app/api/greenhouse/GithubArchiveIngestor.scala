package githubarchive


import java.io.PrintWriter

import com.ning.http.client.{HttpResponseStatus, AsyncHandler, Response}
import dispatch._
import com.github.nscala_time.time.Imports._
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{Path, FileSystem}
import org.joda.time.DateTime
import org.json4s.jackson.JsonMethods._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}
import spray.caching._

object GithubArchiveIngestor {

  /**
   * Valid months: 1-12
   * Valid days: 1-31
   * Valid years: 2011+
   * Valid hours: 0-23
   */
  def pullData(month: Int, day: Int, year: Int, hour: Int)(implicit ctx: ExecutionContext): Future[HourlyData] = {
    val dateTime = DateTime.now.withMonth(month).withDay(day).withYear(year).withHour(hour)
    pullData(dateTime)
  }

  def pullData(dateTime: DateTime)(implicit ctx: ExecutionContext): Future[HourlyData] = {
    assert(dateTime >= DateTime.parse(Config.githubArchiveStartDate))
    val dateSuffix = HourlyData.filePrefix(dateTime)
    val endpoint = s"${Config.githubArchiveUrl}/$dateSuffix.json.gz"

    val query = url(endpoint).setHeaders(Map(
      "Accept-Encoding" -> Seq("gzip")
    ))

    Http(query.OK(as.String)).map { res =>
      HourlyData(dateTime, res)
    }
  }

  def pullAndWrite(dateTime: DateTime)(implicit ctx: ExecutionContext, store: Store[String]): Future[Unit] = {
    pullData(dateTime).map { events =>
      writeDataToStore(events)
    }
  }

  def writeDataToStore(events: HourlyData)(implicit store: Store[String]): Future[Unit] = {
    store.insert(events.path, events.data)
  }
}

case class HourlyData(dateTime: DateTime, data: String) {
  def path: String = s"/${HourlyData.filePrefix(dateTime)}/$hour.json.gz"
  def hour = dateTime.hour.get()
  def date = dateTime.toString("yyyy-MM-dd")
}

object HourlyData {
  def filePrefix(dateTime: DateTime): String = s"${dateTime.toString("yyyy-MM-dd")}-${dateTime.hour.get()}"
}

object DefaultHdfsStore {
  val store = HdfsStore()
  def apply = store
}

case class InMemoryStore[T](cache: Cache[T])(implicit ctx: ExecutionContext) extends Store[T] {
  def insert(path: String, data: T): Future[Unit] = {
    cache(path) {
      Future.successful(data)
    }.map(_ => ())
  }
  def exists(path: String): Future[Boolean] = {
    cache.get(path) match {
      case Some(x) => Future.successful(true)
      case None => Future.successful(false)
    }
  }
}

case class HdfsStore(conf: Configuration = new Configuration()) extends Store[String] {
  val filesystem = {
    FileSystem.get(conf)
  }

  def insert(path: String, data: String): Future[Unit] = {
    val fs = filesystem

    val output = fs.create(new Path(path))
    val writer = new PrintWriter(output)

    Try(writer.write(data)) match {
      case Success(_) =>
        writer.close()
        Future.successful(())
      case Failure(error) =>
        writer.close()
        Future.failed(error)
    }
  }

  def exists(path: String): Future[Boolean] = {
    val fs = filesystem
    Try(fs.exists(new Path(path))) match {
      case Success(bool) =>
        Future.successful(bool)
      case Failure(error) =>
        Future.failed(error)
    }
  }
}


trait Store[T] {
  def insert(path: String, data: T): Future[Unit]
  def exists(path: String): Future[Boolean]
}

case class Repo(url: String, name: String, id: Long)
case class Org(gravatar_id: String, url: String, avatar_url: String)