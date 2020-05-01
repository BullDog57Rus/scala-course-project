package moneymaker.currencies

import java.time.LocalDate

import akka.stream.scaladsl.Source
import moneymaker.models.Entry

import scala.concurrent.Future

trait CurrenciesService {

  def getCurrencies(base: String,
                    currency1: String,
                    currency2: String,
                    dateFrom: LocalDate,
                    dateTo: LocalDate,
                    compare: Boolean): Source[Entry, Any]

  def getSpeeds(base: String,
                currency1: String,
                currency2: String,
                dateFrom: LocalDate,
                dateTo: LocalDate,
                compare: Boolean): Source[Entry, Any]

  def getAccelerations(base: String,
                       currency1: String,
                       currency2: String,
                       dateFrom: LocalDate,
                       dateTo: LocalDate,
                       compare: Boolean): Source[Entry, Any]

  def getMinimumShift(base: String,
                      currency1: String,
                      currency2: String,
                      dateFrom: LocalDate,
                      dateTo: LocalDate,
                      maximumShiftDays: Int): Future[Int]
}
