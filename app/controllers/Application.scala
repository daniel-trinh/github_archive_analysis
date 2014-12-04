package controllers

import org.json4s
import org.json4s.jackson.JsonMethods
import play.api._
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import dispatch.Defaults._
import dispatch.{Http => DispatchHttp, _}
import org.joda.time._
import org.joda.time.format._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization.{ read, write }
import redis.RedisClient
import scala.concurrent.{Promise, Await}
import scala.concurrent.duration._
import scala.util.Success
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._

object Application extends Controller {
  implicit val format = org.json4s.DefaultFormats
  implicit val akkaSystem = akka.actor.ActorSystem()

  val redis = RedisClient(host = "0.0.0.0")

  def ping = Action.async {
    val futurePong = redis.ping()
    futurePong.map(pong => {
      Ok(pong)
    }) recover {
      case e => InternalServerError("Something gone wrong! :(")
    }
  }

  def renderTest = Action {
    Ok("hello")
  }

  def renderFutureTest = Action.async {
    Future.successful(Ok("hello"))
  }

  def redisTest = Action.async {
    val setName = "test"
    val key = 1.337

    val cacheResult = redis.sadd(setName, key).map {
      case 0 => "Cache not updated"
      case _ => "Cache updated"
    }
    cacheResult.map(x => Ok(x))
  }

  def queryTest(elem: Option[List[Int]]) = Action.async {
    Future.successful(Ok(elem.toString))
  }

  def elasticSearchTest = Action.async {
//    val client = ElasticClient.remote("127.0.0.1", 9201)
    val client = ElasticClient.local

    val query = {
      search in "site_placements" types("site_placements") fields("ids") query {
        bool {
          must {
            term("category", "adx_marketplace")
          }
        }
      }
    }
    val wut = client.execute { query }

    wut.map { x =>
      Ok(x.toString)
    }
  }

  def expensiveCommand: Future[Double] = {
    val randomDouble = Promise[Double]()
    akkaSystem.scheduler.scheduleOnce(2 seconds) {
      randomDouble.complete(Success(math.random))
    }
    randomDouble.future
  }
}