package moneymaker.data

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.Http

import scala.concurrent.Future

class HttpClientImpl(implicit actorSystem: ActorSystem) extends HttpClient {

  override def sendRequest(httpRequest: HttpRequest): Future[HttpResponse] =
    Http().singleRequest(httpRequest)
}

object HttpClientImpl {

  def apply()(implicit actorSystem: ActorSystem): HttpClient = new HttpClientImpl()
}
