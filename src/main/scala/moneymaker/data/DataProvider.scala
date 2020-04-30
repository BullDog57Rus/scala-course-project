package moneymaker.data

import java.time.LocalDate

import akka.stream.scaladsl.Source
import moneymaker.models.Entry

trait DataProvider {

  def getRatesInBaseCurrencyWithinPeriod(base: String,
                                         currencies: List[String],
                                         start: LocalDate,
                                         end: LocalDate): Source[Entry, Any]
}
