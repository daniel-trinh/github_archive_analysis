package binders

import play.api.mvc.QueryStringBindable
import java.lang.IllegalArgumentException
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

object DateParamBinder {

  class DateQueryBinder extends QueryStringBindable[DateTime] {
    def bind(key: String, params: Map[String, Seq[String]]) : Option[Either[String,DateTime]]  = {
      params.get(key).flatMap(_.headOption).map { s =>
        dateBindHelper(key, s)
      }
    }
    def unbind(key: String, value: DateTime) : String  = dateUnbindHelper(key, value)
  }

  val ISODateFormatter = ISODateTimeFormat.date()
  val ISODateTimeFormatter = ISODateTimeFormat.dateTime()

  def dateBindHelper(key: String, value: String) : Either[String, DateTime] = {
    val formatters = List(
      ISODateFormatter,
      ISODateTimeFormatter
    )
    val date = formatters.flatMap { formatter =>
      try {
        Some(formatter.parseDateTime(value))
      } catch {
        case e: IllegalArgumentException => None
      }
    }.headOption

    date match {
      case Some(date) => Right(date)
      case None       => Left(s"Could not parse date: $value. Must be in ISO 8601 Format (YYYY-MM-DD).")
    }
  }

  def dateUnbindHelper(key: String, value: DateTime) : String = {
    s"$key=${ISODateFormatter.print(value)}"
  }
}