package ingestor.stores

import java.io.PrintWriter

import com.danieltrinh.ScalaUtilExtensions._

import dispatch._
import ingestor.Config
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{Path, FileSystem}

import scala.concurrent.ExecutionContext
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

case class HdfsStore(conf: Configuration = HdfsStore.defaultConfig)(implicit ctx: ExecutionContext) extends Store[String] {
  val filesystem = {
    FileSystem.get(conf)
  }

  /**
   * Insert data into the HDFS pointed at via conf.
   * @param path File path to store data in
   * @param data Data to insert to Hdfs
   * @param overwrite If passed, any existing file is overwritten if it already exists.
   */
  def insert(path: String, data: String, overwrite: Boolean = true): Future[Unit] = {
    val fs = filesystem

    val hdfsPath = new Path(path)

    val output = Try(fs.create(hdfsPath, overwrite))
    val writer = output.map {o => new PrintWriter(o)}

    val result = writer.map { w =>
      w.write(data)
    }.toFuture

    writer.map { _.close }

    result
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
