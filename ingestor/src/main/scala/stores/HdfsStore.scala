package ingestor.stores

import java.io.PrintWriter

import dispatch._
import ingestor.Config
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{Path, FileSystem}

import scala.util.{Failure, Success, Try}

object HdfsStore {
  def defaultConfig = {
    val hadoopConfig = new Configuration()
    Config.hadoopConfigPaths.foreach { path =>
      hadoopConfig.addResource(new Path(path))
    }
    hadoopConfig
  }
}

case class HdfsStore(conf: Configuration = HdfsStore.defaultConfig) extends Store[String] {
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
