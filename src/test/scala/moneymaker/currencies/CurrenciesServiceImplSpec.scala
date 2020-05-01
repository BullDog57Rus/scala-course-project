package moneymaker.currencies

import java.time.LocalDate
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import moneymaker.data.{DataProvider, StreamProcessing}
import moneymaker.models.Entry
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import org.mockito.captor.ArgCaptor
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration

class CurrenciesServiceImplSpec extends AnyWordSpec with Matchers with MockitoSugar {

  implicit val actors: ActorSystem = ActorSystem()
  implicit val executionContext: ExecutionContext = actors.dispatcher

  val USD = "USD"
  val EUR = "EUR"
  val RUB = "RUB"

  "Get currencies" should {
    "be correct with compare = false" in {
      val mockProvider = mock[DataProvider]
      val testService = CurrenciesServiceImpl(mockProvider)
      val baseCaptor = ArgCaptor[String]
      val currenciesCaptor = ArgCaptor[List[String]]
      val startDateCaptor = ArgCaptor[LocalDate]
      val endDateCaptor = ArgCaptor[LocalDate]

      val today = LocalDate.now()
      val source = Seq(
        Entry(today, Map(RUB -> 15D)),
        Entry(today.plusDays(1), Map(RUB -> 22D)),
        Entry(today.plusDays(2), Map(RUB -> 17D)),
        Entry(today.plusDays(3), Map(RUB -> 11D))
      )

      when(mockProvider.getRatesInBaseCurrencyWithinPeriod(
        baseCaptor, currenciesCaptor, startDateCaptor, endDateCaptor)) thenReturn Source(source)

      val res = testService.getCurrencies(RUB, EUR, USD, today, today.plusDays(3), compare = false)

      baseCaptor.value shouldBe RUB
      currenciesCaptor.value shouldBe List(EUR, USD)
      startDateCaptor.value shouldBe today
      endDateCaptor.value shouldBe today.plusDays(3)

      res
        .runWith(TestSink.probe[Entry])
        .request(4)
        .expectNextN(source)
        .expectComplete()
    }

    "be correct with compare = true" in {
      val mockProvider = mock[DataProvider]
      val testService = CurrenciesServiceImpl(mockProvider)

      val today = LocalDate.now()
      val source = Seq(
        Entry(today, Map(EUR -> 15D, USD -> 14D)),
        Entry(today.plusDays(1), Map(EUR -> 22D, USD -> 20D)),
        Entry(today.plusDays(2), Map(EUR -> 17D, USD -> 18D)),
        Entry(today.plusDays(3), Map(EUR -> 11D, USD -> 13D))
      )

      when(mockProvider.getRatesInBaseCurrencyWithinPeriod(
        any[String], any[List[String]], any[LocalDate], any[LocalDate])) thenReturn Source(source)

      val res = testService.getCurrencies(RUB, EUR, USD, today, today.plusDays(3), compare = true)

      res
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
  }

  "Get speeds" should {
    "be correct" in {
      val mockProvider = mock[DataProvider]
      val testService = CurrenciesServiceImpl(mockProvider)
      val baseCaptor = ArgCaptor[String]
      val currenciesCaptor = ArgCaptor[List[String]]
      val startDateCaptor = ArgCaptor[LocalDate]
      val endDateCaptor = ArgCaptor[LocalDate]

      val today = LocalDate.now()
      val source = Seq(
        Entry(today, Map(RUB -> 15D)),
        Entry(today.plusDays(1), Map(RUB -> 22D)),
        Entry(today.plusDays(2), Map(RUB -> 17D)),
        Entry(today.plusDays(3), Map(RUB -> 11D))
      )

      when(mockProvider.getRatesInBaseCurrencyWithinPeriod(
        baseCaptor, currenciesCaptor, startDateCaptor, endDateCaptor)) thenReturn Source(source)

      val res = testService.getSpeeds(RUB, EUR, USD, today, today.plusDays(3), compare = false)

      baseCaptor.value shouldBe RUB
      currenciesCaptor.value shouldBe List(EUR, USD)
      startDateCaptor.value shouldBe today
      endDateCaptor.value shouldBe today.plusDays(3)

      res
        .runWith(TestSink.probe[Entry])
        .request(3)
        .expectNext(
          Entry(today.plusDays(1), Map(RUB -> 7D)),
          Entry(today.plusDays(2), Map(RUB -> -5D)),
          Entry(today.plusDays(3), Map(RUB -> -6D))
        )
        .expectComplete()
    }
  }

  "Get accelerations" should {
    "be correct" in {
      val mockProvider = mock[DataProvider]
      val testService = CurrenciesServiceImpl(mockProvider)
      val baseCaptor = ArgCaptor[String]
      val currenciesCaptor = ArgCaptor[List[String]]
      val startDateCaptor = ArgCaptor[LocalDate]
      val endDateCaptor = ArgCaptor[LocalDate]

      val today = LocalDate.now()
      val source = Seq(
        Entry(today, Map(RUB -> 15D)),
        Entry(today.plusDays(1), Map(RUB -> 22D)),
        Entry(today.plusDays(2), Map(RUB -> 17D)),
        Entry(today.plusDays(3), Map(RUB -> 11D))
      )

      when(mockProvider.getRatesInBaseCurrencyWithinPeriod(
        baseCaptor, currenciesCaptor, startDateCaptor, endDateCaptor)) thenReturn Source(source)

      val res = testService.getAccelerations(RUB, EUR, USD, today, today.plusDays(3), compare = false)

      baseCaptor.value shouldBe RUB
      currenciesCaptor.value shouldBe List(EUR, USD)
      startDateCaptor.value shouldBe today
      endDateCaptor.value shouldBe today.plusDays(3)

      res
        .runWith(TestSink.probe[Entry])
        .request(3)
        .expectNext(
          Entry(today.plusDays(2), Map(RUB -> -12D)),
          Entry(today.plusDays(3), Map(RUB -> -1D))
        )
        .expectComplete()
    }
  }

  "Get minimum shift" should {
    "be correct" in {
      val mockProvider = mock[DataProvider]
      val testService = CurrenciesServiceImpl(mockProvider)
      val baseCaptor = ArgCaptor[String]
      val currenciesCaptor = ArgCaptor[List[String]]
      val startDateCaptor = ArgCaptor[LocalDate]
      val endDateCaptor = ArgCaptor[LocalDate]

      val today = LocalDate.now()
      val source = Seq(
        Entry(today, Map(EUR -> 15D, USD -> 14D)),
        Entry(today.plusDays(1), Map(EUR -> 22D, USD -> 16D)),
        Entry(today.plusDays(2), Map(EUR -> 17D, USD -> 21D)),
        Entry(today.plusDays(3), Map(EUR -> 11D, USD -> 18D)),
        Entry(today.plusDays(4), Map(EUR -> 12D, USD -> 10D)),
        Entry(today.plusDays(5), Map(EUR -> 9D, USD -> 12D)),
        Entry(today.plusDays(6), Map(EUR -> 14D, USD -> 8D))
      )

      when(mockProvider.getRatesInBaseCurrencyWithinPeriod(
        baseCaptor, currenciesCaptor, startDateCaptor, endDateCaptor)) thenReturn Source(source)

      val resFuture = testService.getMinimumShift(RUB, EUR, USD, today, today.plusDays(3), 2)

      baseCaptor.value shouldBe RUB
      currenciesCaptor.value shouldBe List(EUR, USD)
      startDateCaptor.value shouldBe today
      endDateCaptor.value shouldBe today.plusDays(3)

      val res = Await.result(resFuture, Duration(3, TimeUnit.SECONDS))
      res shouldBe 1
    }
  }
}
