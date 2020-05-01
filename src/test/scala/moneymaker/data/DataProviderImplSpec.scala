package moneymaker.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import akka.stream.testkit.scaladsl.TestSink
import akka.util.ByteString
import moneymaker.models.Entry
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import org.mockito.captor.ArgCaptor
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.{ExecutionContext, Future}

class DataProviderImplSpec extends AnyWordSpec with Matchers with MockitoSugar {

  implicit val actors: ActorSystem = ActorSystem()
  implicit val executionContext: ExecutionContext = actors.dispatcher

  val USD = "USD"
  val EUR = "EUR"
  val RUB = "RUB"
  val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  val returnedJson: String =
    """{
      |    "rates": {
      |        "2020-01-07": {
      |            "EUR": 0.0144388486,
      |            "USD": 0.0161310816
      |        },
      |        "2020-01-08": {
      |            "EUR": 0.014568998,
      |            "USD": 0.0161934413
      |        },
      |        "2020-01-10": {
      |            "EUR": 0.0146970209,
      |            "USD": 0.0163004659
      |        },
      |        "2020-01-02": {
      |            "EUR": 0.0144531019,
      |            "USD": 0.0161773569
      |        },
      |        "2020-01-13": {
      |            "EUR": 0.0146865954,
      |            "USD": 0.016340306
      |        },
      |        "2020-01-15": {
      |            "EUR": 0.0146116373,
      |            "USD": 0.0162802863
      |        },
      |        "2020-01-03": {
      |            "EUR": 0.0144678019,
      |            "USD": 0.0161272588
      |        },
      |        "2020-01-09": {
      |            "EUR": 0.0146895295,
      |            "USD": 0.0163200672
      |        },
      |        "2020-01-14": {
      |            "EUR": 0.0146537753,
      |            "USD": 0.0162876712
      |        },
      |        "2020-01-06": {
      |            "EUR": 0.0144082461,
      |            "USD": 0.0161285907
      |        }
      |    },
      |    "start_at": "2020-01-01",
      |    "base": "RUB",
      |    "end_at": "2020-01-15"
      |}""".stripMargin

  val emptyJson: String =
    """{
      |    "rates": {},
      |    "start_at": "2020-01-01",
      |    "base": "RUB",
      |    "end_at": "2020-01-15"
      |}""".stripMargin

  "Data provider" should {

    "create correct source with data" in {
      val mockHttpClient = mock[HttpClient]
      val testProvider = DataProviderImpl(mockHttpClient)
      val httpCaptor = ArgCaptor[HttpRequest]

      when(mockHttpClient.sendRequest(httpCaptor)) thenReturn
        Future(HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, ByteString(returnedJson))))

      val start = LocalDate.of(2020, 1, 1)
      val end = LocalDate.of(2020, 1, 15)
      val currencies = List(EUR, USD)
      val source = testProvider.getRatesInBaseCurrencyWithinPeriod(RUB, currencies, start, end)

      httpCaptor.value.uri shouldBe Uri("https://api.exchangeratesapi.io/history")
        .withQuery(Query(
          "base" -> RUB,
          "start_at" -> start.format(dateTimeFormatter),
          "end_at" -> end.format(dateTimeFormatter),
          "symbols" -> currencies.mkString(",")
        ))

      val expectedDate1 = LocalDate.of(2020, 1, 2)
      val expectedDate2 = LocalDate.of(2020, 1, 15)
      source
        .filter(e => e.date.equals(expectedDate1) || e.date.equals(expectedDate2))
        .runWith(TestSink.probe[Entry])
        .request(2)
        .expectNextUnordered(
          Entry(expectedDate1, Map(EUR -> 1 / 0.0144531019, USD -> 1 / 0.0161773569)),
          Entry(expectedDate2, Map(EUR -> 1 / 0.0146116373, USD -> 1 / 0.0162802863))
        )
        .expectComplete()
    }

    "create correct source with no data" in {
      val mockHttpClient = mock[HttpClient]
      val testProvider = DataProviderImpl(mockHttpClient)
      val httpCaptor = ArgCaptor[HttpRequest]
      when(mockHttpClient.sendRequest(httpCaptor)) thenReturn
        Future(HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, ByteString(emptyJson))))

      val start = LocalDate.of(2020, 1, 1)
      val end = LocalDate.of(2020, 1, 15)
      val currencies = List(EUR, USD)
      val source = testProvider.getRatesInBaseCurrencyWithinPeriod(RUB, currencies, start, end)

      httpCaptor.value.uri shouldBe Uri("https://api.exchangeratesapi.io/history")
        .withQuery(Query(
          "base" -> RUB,
          "start_at" -> start.format(dateTimeFormatter),
          "end_at" -> end.format(dateTimeFormatter),
          "symbols" -> currencies.mkString(",")
        ))

      source
        .runWith(TestSink.probe[Entry])
        .request(1)
        .expectComplete()
    }

    "create correct source with bad data" in {
      val mockHttpClient = mock[HttpClient]
      val testProvider = DataProviderImpl(mockHttpClient)
      when(mockHttpClient.sendRequest(any[HttpRequest])) thenReturn
        Future(HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, ByteString("Nikita eto bad case"))))

      val start = LocalDate.of(2020, 1, 1)
      val end = LocalDate.of(2020, 1, 15)
      val currencies = List(EUR, USD)
      val source = testProvider.getRatesInBaseCurrencyWithinPeriod(RUB, currencies, start, end)

      source
        .runWith(TestSink.probe[Entry])
        .request(1)
        .expectError()
    }
  }
}
