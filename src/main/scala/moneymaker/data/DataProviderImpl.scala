package moneymaker.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.stream.alpakka.json.scaladsl.JsonReader
import akka.stream.scaladsl.Source
import io.circe.{KeyDecoder, parser}
import moneymaker.models.Entry

import scala.concurrent.ExecutionContext

class DataProviderImpl(httpClient: HttpClient)(implicit ac: ActorSystem, ec: ExecutionContext) extends DataProvider {

  private val apiUri = Uri("https://api.exchangeratesapi.io/history")
  private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  implicit val localDateKeyDecoder: KeyDecoder[LocalDate] = (key: String) => Some(LocalDate.parse(key, dateFormatter))

  override def getRatesInBaseCurrencyWithinPeriod(base: String,
                                                  currencies: List[String],
                                                  start: LocalDate,
                                                  end: LocalDate): Source[Entry, Any] = {
    val responseFuture = httpClient.sendRequest(HttpRequest(uri = apiUri.withQuery(Query(
      "base" -> base,
      "start_at" -> start.format(dateFormatter),
      "end_at" -> end.format(dateFormatter),
      "symbols" -> currencies.mkString(",")
    ))))
    Source.futureSource(responseFuture.map(_.entity.dataBytes))
      .via(JsonReader.select("$.rates"))
      .map(x => parser.decode[Map[LocalDate, Map[String, Double]]](x.utf8String))
      .mapConcat {
        case Right(value) => value.toList.map(x => Entry(x._1, x._2.map {
          case (currency, inversePrice) => (currency, 1 / inversePrice)
        })).sortBy(_.date)
        case Left(_) => List.empty
      }
  }
}

object DataProviderImpl {

  def apply(implicit ac: ActorSystem, ec: ExecutionContext): DataProviderImpl =
    new DataProviderImpl(HttpClientImpl())

  def apply(httpClient: HttpClient)(implicit ac: ActorSystem, ec: ExecutionContext): DataProviderImpl =
    new DataProviderImpl(httpClient)
}
