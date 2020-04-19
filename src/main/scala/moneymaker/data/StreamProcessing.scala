package moneymaker.data

import akka.stream.alpakka.csv.scaladsl.CsvFormatting
import akka.stream.scaladsl.{Flow, Sink}
import akka.util.ByteString
import cats.implicits._
import moneymaker.models.Entry

import scala.concurrent.Future

object StreamProcessing {

  val formatter: Flow[Vector[String], ByteString, Any] = CsvFormatting.format()

  val calculateDerivative: Flow[Entry, Entry, Any] = Flow[Entry].sliding(2).map {
    window =>
      val today = window.last
      val yesterday = window.head
      val derivatives = today.currencies.map { case (currency, price) =>
        (currency, price - yesterday.currencies.getOrElse(currency, 0D))
      }
      Entry(today.date, derivatives)
  }

  def calculateDifferenceInCurrenciesIfNeeded(baseCurrency: String,
                                              comparativeCurrency: String,
                                              compare: Boolean): Flow[Entry, Entry, Any] =
    if (compare) Flow[Entry].map {
      entry =>
        Entry(entry.date,
          Map(baseCurrency + " vs. " + comparativeCurrency ->
            (entry.currencies.getOrElse(baseCurrency, 0D) - entry.currencies.getOrElse(comparativeCurrency, 0D))
          )
        )
    } else Flow[Entry]

  def calculateShiftedValues(baseCurrency: String,
                             comparativeCurrency: String,
                             maximumShiftDays: Int): Flow[Entry, Map[Int, Double], Any] =
    Flow[Entry].sliding(2 * maximumShiftDays + 1).map {
      window =>
        val normalizedMaximumShiftDays = window.length / 2
        calculateDifferenceInsideWindowForValue(
          window.map(x => x.currencies.getOrElse(comparativeCurrency, 0)),
          -normalizedMaximumShiftDays to normalizedMaximumShiftDays,
          window(normalizedMaximumShiftDays).currencies.getOrElse(baseCurrency, 0),
          Map()
        )
    }

  @scala.annotation.tailrec
  private def calculateDifferenceInsideWindowForValue(window: Seq[Double],
                                                      counter: Seq[Int],
                                                      value: Double,
                                                      result: Map[Int, Double]): Map[Int, Double] = {
    if (window.isEmpty || counter.isEmpty) return result
    calculateDifferenceInsideWindowForValue(window.tail, counter.tail, value,
      result + (counter.head -> (value - window.head)))
  }

  val combineMaps: Sink[Map[Int, Double], Future[Map[Int, Double]]] =
    Sink.fold(Map.empty[Int, Double])(_ |+| _)
}
