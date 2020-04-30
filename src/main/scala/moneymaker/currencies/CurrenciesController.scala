package moneymaker.currencies

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.scaladsl.Keep
import moneymaker.data.{DataProvider, DataProviderImpl, StreamProcessing}

import scala.concurrent.ExecutionContext

class CurrenciesController(dataProvider: DataProvider)(implicit ac: ActorSystem, ec: ExecutionContext) {

  val currenciesPath: String = "currencies"
  val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

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
          val stream = dataProvider.getRatesInBaseCurrencyWithinPeriod(base, List(currency1, currency2), dateFrom, dateTo)
            .via(StreamProcessing.calculateDifferenceInCurrenciesIfNeeded(currency1, currency2, compare))
            .map(_.toVector(dateFormatter))
            .via(StreamProcessing.formatter)
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
          val stream = dataProvider.getRatesInBaseCurrencyWithinPeriod(base, List(currency1, currency2), dateFrom, dateTo)
            .via(StreamProcessing.calculateDifferenceInCurrenciesIfNeeded(currency1, currency2, compare))
            .via(StreamProcessing.calculateDerivative)
            .map(_.toVector(dateFormatter))
            .via(StreamProcessing.formatter)
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
          val stream = dataProvider.getRatesInBaseCurrencyWithinPeriod(base, List(currency1, currency2), dateFrom, dateTo)
            .via(StreamProcessing.calculateDifferenceInCurrenciesIfNeeded(currency1, currency2, compare))
            .via(StreamProcessing.calculateDerivative)
            .via(StreamProcessing.calculateDerivative)
            .map(_.toVector(dateFormatter))
            .via(StreamProcessing.formatter)
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
          val stream = dataProvider.getRatesInBaseCurrencyWithinPeriod(base, List(currency1, currency2), dateFrom, dateTo)
            .via(StreamProcessing.calculateShiftedValues(currency1, currency2, maximumShiftDays))
            .toMat(StreamProcessing.combineMaps)(Keep.right).run().map(_.minBy(_._2.abs)._1)
          complete(StatusCodes.OK, stream.map(_.toString))
        }
    }
}

object CurrenciesController {

  def apply(implicit ac: ActorSystem, ec: ExecutionContext): CurrenciesController =
    new CurrenciesController(DataProviderImpl(ac, ec))

  def apply(dataProvider: DataProvider)(implicit ac: ActorSystem, ec: ExecutionContext): CurrenciesController =
    new CurrenciesController(dataProvider)
}
