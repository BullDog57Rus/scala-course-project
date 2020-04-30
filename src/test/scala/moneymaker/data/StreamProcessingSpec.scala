package moneymaker.data

import java.time.LocalDate
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Keep, Source}
import akka.stream.testkit.scaladsl.TestSink
import moneymaker.models.Entry
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class StreamProcessingSpec extends AnyFlatSpec with Matchers {

  implicit val actorSystem: ActorSystem = ActorSystem()

  val USD = "USD"
  val EUR = "EUR"
  val RUB = "RUB"

  "Derivative" should "be correct" in {
    val today = LocalDate.now()
    val source = Source(Seq(
      Entry(today, Map(RUB -> 15D)),
      Entry(today.plusDays(1), Map(RUB -> 22D)),
      Entry(today.plusDays(2), Map(RUB -> 17D)),
      Entry(today.plusDays(3), Map(RUB -> 11D))))

    source
      .via(StreamProcessing.calculateDerivative)
      .runWith(TestSink.probe[Entry])
      .request(3)
      .expectNext(
        Entry(today.plusDays(1), Map(RUB -> 7D)),
        Entry(today.plusDays(2), Map(RUB -> -5D)),
        Entry(today.plusDays(3), Map(RUB -> -6D)))
      .expectComplete()
  }

  "Difference in currencies" should "be correct if compare == true" in {
    val today = LocalDate.now()
    val source = Source(Seq(
      Entry(today, Map(EUR -> 15D, USD -> 14D)),
      Entry(today.plusDays(1), Map(EUR -> 22D, USD -> 20D)),
      Entry(today.plusDays(2), Map(EUR -> 17D, USD -> 18D)),
      Entry(today.plusDays(3), Map(EUR -> 11D, USD -> 13D))))

    source
      .via(StreamProcessing.calculateDifferenceInCurrenciesIfNeeded(EUR, USD, compare = true))
      .runWith(TestSink.probe[Entry])
      .request(4)
      .expectNext(
        Entry(today, Map(EUR + StreamProcessing.VS + USD -> 1D)),
        Entry(today.plusDays(1), Map(EUR + StreamProcessing.VS + USD -> 2D)),
        Entry(today.plusDays(2), Map(EUR + StreamProcessing.VS + USD -> -1D)),
        Entry(today.plusDays(3), Map(EUR + StreamProcessing.VS + USD -> -2D))
      )
      .expectComplete()
  }

  "Difference in currencies" should "be correct if compare == false" in {
    val today = LocalDate.now()
    val source = Source(Seq(
      Entry(today, Map(EUR -> 15D, USD -> 14D)),
      Entry(today.plusDays(1), Map(EUR -> 22D, USD -> 20D)),
      Entry(today.plusDays(2), Map(EUR -> 17D, USD -> 18D)),
      Entry(today.plusDays(3), Map(EUR -> 11D, USD -> 13D))))

    source
      .via(StreamProcessing.calculateDifferenceInCurrenciesIfNeeded(EUR, USD, compare = false))
      .runWith(TestSink.probe[Entry])
      .request(4)
      .expectNext(
        Entry(today, Map(EUR -> 15D, USD -> 14D)),
        Entry(today.plusDays(1), Map(EUR -> 22D, USD -> 20D)),
        Entry(today.plusDays(2), Map(EUR -> 17D, USD -> 18D)),
        Entry(today.plusDays(3), Map(EUR -> 11D, USD -> 13D))
      )
      .expectComplete()
  }

  "Shifted days" should "be correct" in {
    val today = LocalDate.now()
    val source = Source(Seq(
      Entry(today, Map(EUR -> 15D, USD -> 14D)),
      Entry(today.plusDays(1), Map(EUR -> 22D, USD -> 16D)),
      Entry(today.plusDays(2), Map(EUR -> 17D, USD -> 21D)),
      Entry(today.plusDays(3), Map(EUR -> 11D, USD -> 18D)),
      Entry(today.plusDays(4), Map(EUR -> 12D, USD -> 10D)),
      Entry(today.plusDays(5), Map(EUR -> 9D, USD -> 12D)),
      Entry(today.plusDays(6), Map(EUR -> 14D, USD -> 8D))))

    source
      .via(StreamProcessing.calculateShiftedValues(EUR, USD, 2))
      .runWith(TestSink.probe[Map[Int, Double]])
      .request(3)
      .expectNext(
        Map(-2 -> 3D, -1 -> 1D, 0 -> -4D, 1 -> -1D, 2 -> 7D),
        Map(-2 -> -5D, -1 -> -10D, 0 -> -7D, 1 -> 1D, 2 -> -1D),
        Map(-2 -> -9D, -1 -> -6D, 0 -> 2D, 1 -> 0D, 2 -> 4D)
      )
      .expectComplete()
  }

  "Combine maps" should "be correct" in {
    val today = LocalDate.now()
    val source = Source(Seq(
      Entry(today, Map(EUR -> 15D, USD -> 14D)),
      Entry(today.plusDays(1), Map(EUR -> 22D, USD -> 16D)),
      Entry(today.plusDays(2), Map(EUR -> 17D, USD -> 21D)),
      Entry(today.plusDays(3), Map(EUR -> 11D, USD -> 18D)),
      Entry(today.plusDays(4), Map(EUR -> 12D, USD -> 10D)),
      Entry(today.plusDays(5), Map(EUR -> 9D, USD -> 12D)),
      Entry(today.plusDays(6), Map(EUR -> 14D, USD -> 8D))))

    val resFuture = source
      .via(StreamProcessing.calculateShiftedValues(EUR, USD, 2))
      .toMat(StreamProcessing.combineMaps)(Keep.right)
      .run()
    val res = Await.result(resFuture, Duration(3, TimeUnit.SECONDS))
    res shouldBe Map(-2 -> -11D, -1 -> -15D, 0 -> -9D, 1 -> 0D, 2 -> 10D)
  }
}
