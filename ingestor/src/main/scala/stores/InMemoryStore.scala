package ingestor.stores

import java.io.IOException

import dispatch._
import spray.caching.Cache

import scala.concurrent.ExecutionContext

case class InMemoryStore[T](cache: Cache[T])(implicit ctx: ExecutionContext) extends Store[T] {
  def insert(path: String, data: T, overwrite: Boolean = true): Future[Unit] = {
    if (overwrite) {
      cache(path) {
        Future.successful(data)
      }.map(_ => ())
    } else {
      cache.get(path) match {
        case Some(x) =>
          Future.failed(new IOException(s"Data with path $path already exists in the store, and overwrite has been set to false."))
        case None =>
          cache(path) {
            Future.successful(data)
          }.map(_ => ())
      }
    }
  }
  def exists(path: String): Future[Boolean] = {
    cache.get(path) match {
      case Some(x) => Future.successful(true)
      case None => Future.successful(false)
    }
  }
}