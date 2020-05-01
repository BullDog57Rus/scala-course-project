package moneymaker.currencies

import java.time.LocalDate

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Keep, Source}
import moneymaker.data.{DataProvider, DataProviderImpl, StreamProcessing}
import moneymaker.models.Entry

import scala.concurrent.{ExecutionContext, Future}

class CurrenciesServiceImpl(dataProvider: DataProvider)(implicit ac: ActorSystem, ec: ExecutionContext) extends CurrenciesService {

  override def getCurrencies(base: String,
                             currency1: String,
                             currency2: String,
                             dateFrom: LocalDate,
                             dateTo: LocalDate,
                             compare: Boolean): Source[Entry, Any] =
    dataProvider.getRatesInBaseCurrencyWithinPeriod(base, List(currency1, currency2), dateFrom, dateTo)
      .via(StreamProcessing.calculateDifferenceInCurrenciesIfNeeded(currency1, currency2, compare))

  override def getSpeeds(base: String,
                         currency1: String,
                         currency2: String,
                         dateFrom: LocalDate,
                         dateTo: LocalDate,
                         compare: Boolean): Source[Entry, Any] =
    dataProvider.getRatesInBaseCurrencyWithinPeriod(base, List(currency1, currency2), dateFrom, dateTo)
      .via(StreamProcessing.calculateDifferenceInCurrenciesIfNeeded(currency1, currency2, compare))
      .via(StreamProcessing.calculateDerivative)

  override def getAccelerations(base: String,
                                currency1: String,
                                currency2: String,
                                dateFrom: LocalDate,
                                dateTo: LocalDate,
                                compare: Boolean): Source[Entry, Any] =
    dataProvider.getRatesInBaseCurrencyWithinPeriod(base, List(currency1, currency2), dateFrom, dateTo)
      .via(StreamProcessing.calculateDifferenceInCurrenciesIfNeeded(currency1, currency2, compare))
      .via(StreamProcessing.calculateDerivative)
      .via(StreamProcessing.calculateDerivative)

  override def getMinimumShift(base: String,
                               currency1: String,
                               currency2: String,
                               dateFrom: LocalDate,
                               dateTo: LocalDate,
                               maximumShiftDays: Int): Future[Int] =
    dataProvider.getRatesInBaseCurrencyWithinPeriod(base, List(currency1, currency2), dateFrom, dateTo)
      .via(StreamProcessing.calculateShiftedValues(currency1, currency2, maximumShiftDays))
      .toMat(StreamProcessing.combineMaps)(Keep.right).run().map(_.minBy(_._2.abs)._1)
}

object CurrenciesServiceImpl {

  def apply()(implicit ac: ActorSystem, ec: ExecutionContext): CurrenciesService =
    new CurrenciesServiceImpl(DataProviderImpl())

  def apply(dataProvider: DataProvider)(implicit ac: ActorSystem, ec: ExecutionContext): CurrenciesService =
    new CurrenciesServiceImpl(dataProvider)
}