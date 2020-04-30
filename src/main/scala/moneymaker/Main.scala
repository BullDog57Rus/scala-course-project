package moneymaker

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import moneymaker.currencies.CurrenciesController
import moneymaker.data.{DataProviderImpl, HttpClientImpl}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

object Main extends App {

  implicit val actors: ActorSystem = ActorSystem()
  implicit val executionContext: ExecutionContext = actors.dispatcher

  val dataProvider = DataProviderImpl(HttpClientImpl())

  for {
    binding <- Http().bindAndHandle(CurrenciesController(dataProvider).currencies, "localhost", 8080)
    _ = sys.addShutdownHook {
      for {
        _ <- binding.terminate(Duration(5, TimeUnit.SECONDS))
        _ <- actors.terminate()
      } yield ()
    }
  } yield ()
}
