package binders

import play.api.mvc.QueryStringBindable

object QueryBinders {
  type OptionList[T] = Option[List[T]]

  implicit def dateTimeQueryBinder = new DateParamBinder.DateQueryBinder
  implicit def listCsvBinder[T: QueryStringBindable] = new CSVQueryToListBinder[T]
}
