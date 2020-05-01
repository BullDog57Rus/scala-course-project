package moneymaker.currencies

import java.time.{LocalDate, LocalDateTime}
import java.time.format.DateTimeFormatter

import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{HttpRequest, StatusCodes, Uri}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.alpakka.csv.scaladsl.CsvParsing
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import akka.util.ByteString
import moneymaker.models.Entry
import org.mockito.MockitoSugar
import org.mockito.captor.ArgCaptor
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.Future
import scala.util.Try


class CurrenciesControllerSpec extends AnyWordSpec with MockitoSugar with Matchers with ScalatestRouteTest {

  val USD = "USD"
  val EUR = "EUR"
  val RUB = "RUB"

  val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

  def fromString(line: List[String]): Option[Entry] =
    line match {
      case dateString +: entries =>
        for {
          date <- Try(LocalDate.from(dateTimeFormatter.parse(dateString))).toOption
          currencies <- Some(entries.map(str => {
            val arr = str.split(" -> ")
            val currency = arr(0)
            val value = arr(1)
            currency -> value.toDouble
          }).toMap)
        } yield Entry(date, currencies)
    }

  val currenciesPath = "/currencies"

  "Get currencies" should {
    "return 200 ok with body" in {
      val mockService = mock[CurrenciesService]
      val testController = CurrenciesController(mockService)
      val baseCaptor = ArgCaptor[String]
      val currency1Captor = ArgCaptor[String]
      val currency2Captor = ArgCaptor[String]
      val startDateCaptor = ArgCaptor[LocalDate]
      val endDateCaptor = ArgCaptor[LocalDate]
      val compareCaptor = ArgCaptor[Boolean]

      val today = LocalDate.now()
      val source = Seq(
        Entry(today, Map(EUR -> 15D, USD -> 14D)),
        Entry(today.plusDays(1), Map(EUR -> 22D, USD -> 20D)),
        Entry(today.plusDays(2), Map(EUR -> 17D, USD -> 18D)),
        Entry(today.plusDays(3), Map(EUR -> 11D, USD -> 13D))
      )

      val request = HttpRequest(uri = Uri(currenciesPath)
        .withQuery(
          Query(
            "base" -> RUB,
            "currency1" -> EUR,
            "currency2" -> USD,
            "dateFrom" -> today.format(dateTimeFormatter),
            "dateTo" -> today.plusDays(15).format(dateTimeFormatter),
            "compare" -> true.toString)
        )
      )

      when(mockService.getCurrencies(
        baseCaptor, currency1Captor, currency2Captor, startDateCaptor, endDateCaptor, compareCaptor
      )) thenReturn Source(source)

      request ~> Route.seal(testController.getCurrencies) ~> check {
        val res = Source(responseAs[String].split("\n").map(x => ByteString(x)))
          .via(CsvParsing.lineScanner())
          .map(x => fromString(x.map(_.utf8String)))
          .collect { case Some(v) => v }

        res.runWith(TestSink.probe[Entry])
          .request(4)
          .expectNextUnorderedN(
            source
          )
          .expectComplete()
      }
    }
  }

  "Get speeds" should {
    "return 200 ok with body" in {
      val mockService = mock[CurrenciesService]
      val testController = CurrenciesController(mockService)
      val baseCaptor = ArgCaptor[String]
      val currency1Captor = ArgCaptor[String]
      val currency2Captor = ArgCaptor[String]
      val startDateCaptor = ArgCaptor[LocalDate]
      val endDateCaptor = ArgCaptor[LocalDate]
      val compareCaptor = ArgCaptor[Boolean]

      val today = LocalDate.now()
      val source = Seq(
        Entry(today, Map(EUR -> 15D, USD -> 14D)),
        Entry(today.plusDays(1), Map(EUR -> 22D, USD -> 20D)),
        Entry(today.plusDays(2), Map(EUR -> 17D, USD -> 18D)),
        Entry(today.plusDays(3), Map(EUR -> 11D, USD -> 13D))
      )

      val request = HttpRequest(uri = Uri(currenciesPath + "/speeds")
        .withQuery(
          Query(
            "base" -> RUB,
            "currency1" -> EUR,
            "currency2" -> USD,
            "dateFrom" -> today.format(dateTimeFormatter),
            "dateTo" -> today.plusDays(15).format(dateTimeFormatter),
            "compare" -> true.toString)
        )
      )

      when(mockService.getSpeeds(
        baseCaptor, currency1Captor, currency2Captor, startDateCaptor, endDateCaptor, compareCaptor
      )) thenReturn Source(source)

      request ~> Route.seal(testController.getSpeeds) ~> check {
        val res = Source(responseAs[String].split("\n").map(x => ByteString(x)))
          .via(CsvParsing.lineScanner())
          .map(x => fromString(x.map(_.utf8String)))
          .collect { case Some(v) => v }

        res.runWith(TestSink.probe[Entry])
          .request(4)
          .expectNextUnorderedN(
            source
          )
          .expectComplete()
      }
    }
  }

  "Get accelerations" should {
    "return 200 ok with body" in {
      val mockService = mock[CurrenciesService]
      val testController = CurrenciesController(mockService)
      val baseCaptor = ArgCaptor[String]
      val currency1Captor = ArgCaptor[String]
      val currency2Captor = ArgCaptor[String]
      val startDateCaptor = ArgCaptor[LocalDate]
      val endDateCaptor = ArgCaptor[LocalDate]
      val compareCaptor = ArgCaptor[Boolean]

      val today = LocalDate.now()
      val source = Seq(
        Entry(today, Map(EUR -> 15D, USD -> 14D)),
        Entry(today.plusDays(1), Map(EUR -> 22D, USD -> 20D)),
        Entry(today.plusDays(2), Map(EUR -> 17D, USD -> 18D)),
        Entry(today.plusDays(3), Map(EUR -> 11D, USD -> 13D))
      )

      val request = HttpRequest(uri = Uri(currenciesPath + "/accelerations")
        .withQuery(
          Query(
            "base" -> RUB,
            "currency1" -> EUR,
            "currency2" -> USD,
            "dateFrom" -> today.format(dateTimeFormatter),
            "dateTo" -> today.plusDays(15).format(dateTimeFormatter),
            "compare" -> true.toString)
        )
      )

      when(mockService.getAccelerations(
        baseCaptor, currency1Captor, currency2Captor, startDateCaptor, endDateCaptor, compareCaptor
      )) thenReturn Source(source)

      request ~> Route.seal(testController.getAccelerations) ~> check {
        val res = Source(responseAs[String].split("\n").map(x => ByteString(x)))
          .via(CsvParsing.lineScanner())
          .map(x => fromString(x.map(_.utf8String)))
          .collect { case Some(v) => v }

        res.runWith(TestSink.probe[Entry])
          .request(4)
          .expectNextUnorderedN(
            source
          )
          .expectComplete()
      }
    }
  }

  "Get minimum shift" should {
    "return 200 ok with body" in {
      val mockService = mock[CurrenciesService]
      val testController = CurrenciesController(mockService)
      val baseCaptor = ArgCaptor[String]
      val currency1Captor = ArgCaptor[String]
      val currency2Captor = ArgCaptor[String]
      val startDateCaptor = ArgCaptor[LocalDate]
      val endDateCaptor = ArgCaptor[LocalDate]
      val minimumShiftDaysCaptor = ArgCaptor[Int]

      when(mockService.getMinimumShift(
        baseCaptor, currency1Captor, currency2Captor, startDateCaptor, endDateCaptor, minimumShiftDaysCaptor)) thenReturn Future.successful(5)

      val today = LocalDate.now()

      val request = HttpRequest(uri = Uri(currenciesPath + "/shift")
        .withQuery(
          Query(
            "base" -> RUB,
            "currency1" -> EUR,
            "currency2" -> USD,
            "dateFrom" -> today.format(dateTimeFormatter),
            "dateTo" -> today.plusDays(15).format(dateTimeFormatter),
            "maximumShiftDays" -> 10.toString)
        )
      )

      request ~> Route.seal(testController.getMinimumShift) ~> check {
        status shouldBe StatusCodes.OK
        responseAs[String] shouldBe "5"
        baseCaptor.value shouldBe RUB
        currency1Captor.value shouldBe EUR
        currency2Captor.value shouldBe USD
        startDateCaptor.value shouldBe today
        endDateCaptor.value shouldBe today.plusDays(15)
        minimumShiftDaysCaptor.value shouldBe 10
      }
    }
  }
}
