package moneymaker.data

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.Http

import scala.concurrent.Future

class HttpClientImpl extends HttpClient {

  override def sendRequest(httpRequest: HttpRequest)(implicit actorSystem: ActorSystem): Future[HttpResponse] =
    Http().singleRequest(httpRequest)
}

object HttpClientImpl {

  def apply(): HttpClientImpl = new HttpClientImpl()
}
