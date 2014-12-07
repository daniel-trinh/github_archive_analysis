package com.danieltrinh.githubanalysis.stores

import dispatch._
import spray.caching.Cache

import scala.concurrent.ExecutionContext

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
