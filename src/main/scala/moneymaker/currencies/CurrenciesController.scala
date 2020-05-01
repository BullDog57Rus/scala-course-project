package moneymaker.currencies

import java.time.LocalDate

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.alpakka.csv.scaladsl.CsvFormatting
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import moneymaker.Parameters.dateFormatter

import scala.concurrent.ExecutionContext

class CurrenciesController(currenciesService: CurrenciesService)(implicit ac: ActorSystem, ec: ExecutionContext) {

  val currenciesPath: String = "currencies"
  val formatter: Flow[Vector[String], ByteString, Any] = CsvFormatting.format()

  implicit val unmarshalLocalDate: Unmarshaller[String, LocalDate] = Unmarshaller.strict(localDateString =>
    LocalDate.parse(localDateString, dateFormatter))

  def currencies(implicit ac: ActorSystem, ec: ExecutionContext): Route = getCurrencies ~ getSpeeds ~ getAccelerations ~
    getMinimumShift

  val getCurrencies: Route =
    (path(currenciesPath)
      & parameter("base".as[String] ? "RUB")
      & parameter("currency1".as[String])
      & parameter("currency2".as[String])
      & parameter("dateFrom".as[LocalDate])
      & parameter("dateTo".as[LocalDate])
      & parameter("compare".as[Boolean] ? false)) {
      (base, currency1, currency2, dateFrom, dateTo, compare) =>
        get {
          val stream = currenciesService.getCurrencies(base, currency1, currency2, dateFrom, dateTo, compare)
            .map(_.toVector(dateFormatter))
            .via(formatter)
          complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, stream))
        }
    }

  val getSpeeds: Route =
    (pathPrefix(currenciesPath) & path("speeds")
      & parameter("base".as[String] ? "RUB")
      & parameter("currency1".as[String])
      & parameter("currency2".as[String])
      & parameter("dateFrom".as[LocalDate])
      & parameter("dateTo".as[LocalDate])
      & parameter("compare".as[Boolean] ? false)) {
      (base, currency1, currency2, dateFrom, dateTo, compare) =>
        get {
          val stream = currenciesService.getSpeeds(base, currency1, currency2, dateFrom, dateTo, compare)
            .map(_.toVector(dateFormatter))
            .via(formatter)
          complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, stream))
        }
    }

  val getAccelerations: Route =
    (pathPrefix(currenciesPath) & path("accelerations")
      & parameter("base".as[String] ? "RUB")
      & parameter("currency1".as[String])
      & parameter("currency2".as[String])
      & parameter("dateFrom".as[LocalDate])
      & parameter("dateTo".as[LocalDate])
      & parameter("compare".as[Boolean] ? false)) {
      (base, currency1, currency2, dateFrom, dateTo, compare) =>
        get {
          val stream = currenciesService.getAccelerations(base, currency1, currency2, dateFrom, dateTo, compare)
            .map(_.toVector(dateFormatter))
            .via(formatter)
          complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, stream))
        }
    }

  val getMinimumShift: Route =
    (pathPrefix(currenciesPath) & path("shift")
      & parameter("base".as[String] ? "RUB")
      & parameter("currency1".as[String])
      & parameter("currency2".as[String])
      & parameter("dateFrom".as[LocalDate])
      & parameter("dateTo".as[LocalDate])
      & parameter("maximumShiftDays".as[Int])) {
      (base, currency1, currency2, dateFrom, dateTo, maximumShiftDays) =>
        get {
          val stream = currenciesService.getMinimumShift(base, currency1, currency2, dateFrom, dateTo, maximumShiftDays)
          complete(StatusCodes.OK, stream.map(_.toString))
        }
    }
}

object CurrenciesController {

  def apply()(implicit ac: ActorSystem, ec: ExecutionContext): CurrenciesController =
    new CurrenciesController(CurrenciesServiceImpl())

  def apply(currenciesService: CurrenciesService)(implicit ac: ActorSystem, ec: ExecutionContext): CurrenciesController =
    new CurrenciesController(currenciesService)
}
