package moneymaker.data

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}

import scala.concurrent.Future

trait HttpClient {

  def sendRequest(httpRequest: HttpRequest): Future[HttpResponse]
}