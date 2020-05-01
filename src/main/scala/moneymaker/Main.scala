package moneymaker

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import moneymaker.currencies.{CurrenciesController, CurrenciesServiceImpl}
import moneymaker.data.{DataProviderImpl, HttpClientImpl}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

object Main extends App {

  implicit val actors: ActorSystem = ActorSystem()
  implicit val executionContext: ExecutionContext = actors.dispatcher

  val httpClient = HttpClientImpl()
  val dataProvider = DataProviderImpl(httpClient)
  val currenciesService = CurrenciesServiceImpl(dataProvider)

  for {
    binding <- Http().bindAndHandle(CurrenciesController(currenciesService).currencies, "localhost", 8080)
    _ = sys.addShutdownHook {
      for {
        _ <- binding.terminate(Duration(5, TimeUnit.SECONDS))
        _ <- actors.terminate()
      } yield ()
    }
  } yield ()
}
