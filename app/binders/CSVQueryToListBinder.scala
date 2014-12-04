package binders

import play.api.mvc.QueryStringBindable
import QueryBinders.OptionList

/**
 * Converts CSVs in GET Params into an [[OptionList[T]].
 * {{{
 *   "?list=123,456,789&list=34,54" => OptionList(Some(List(123,456,789,34,54))
 * }}}
 */
class CSVQueryToListBinder[T: QueryStringBindable] extends QueryStringBindable[OptionList[T]] {

  def bind(key: String, params: Map[String, Seq[String]]) = Some(Right(bindList[T](key, params)))
  def unbind(key: String, values: OptionList[T]) = unbindList(key, values)

  private def bindList[T: QueryStringBindable](key: String, params: Map[String, Seq[String]]): OptionList[T] = {
    params.get(key) match {
      case Some(elems) =>
        val keyParams = params.getOrElse(key, Seq[String]())

        val rawValues = keyParams.foldLeft(List[String]()) { (boundCSVs, csv) =>
          csv.split(",").filter(_ != "").toList ++ boundCSVs
        }

        val listItems = for {
          rawValue <- rawValues
          bound <- implicitly[QueryStringBindable[T]].bind(key, Map(key -> Seq(rawValue)))
          value <- bound.right.toOption
        } yield value

        Some(listItems)
      case None => None
    }
  }

  private def unbindList[T: QueryStringBindable](key: String, values: OptionList[T]): String = {
    val stringifiedValues = values.getOrElse(List()) map { implicitly[QueryStringBindable[T]].unbind(key, _) }
    stringifiedValues.mkString(",")
  }
}
