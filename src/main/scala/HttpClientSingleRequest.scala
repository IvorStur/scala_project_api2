import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethod, HttpMethods, HttpRequest, RequestEntity}
import akka.http.scaladsl.unmarshalling.Unmarshal

import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import spray.json._
import DefaultJsonProtocol._
import akka.stream.ActorMaterializer

import java.net.URLEncoder
import scala.concurrent.duration.Duration

object HttpClientSingleRequest extends App {

  implicit val as: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = as.dispatcher

  case class Response(status: Int, body: String)

  implicit val passengerFormat = jsonFormat2(Passenger)

  def request(method: HttpMethod, path: String = "", params: Option[String] = None, body: Option[RequestEntity] = None): HttpRequest =
    HttpRequest(
      method = method,
      uri = s"http://localhost:8080$path${params.map(p => s"?$p").getOrElse("")}",
      headers = Seq(),
      entity = body.getOrElse(HttpEntity.Empty)
    )

  implicit class UltraHttpRequest(request: HttpRequest) {
    def exec: Future[Response] =
      for {
        response <- Http().singleRequest(request)
        status = response.status.intValue()
        body <- Unmarshal(response.entity).to[String]
        _ = response.entity.discardBytes()
      } yield Response(status, body)
  }

  val passengerName = "John Doe"
  val encodedPassengerName = URLEncoder.encode(passengerName, "UTF-8")
  val runF = request(HttpMethods.GET, path = s"/passenger/$encodedPassengerName")
    .exec
    .map(response => {
      println(s"Response body: ${response.body}")
      val passenger = response.body.parseJson.convertTo[Passenger]
      println(s"Received passenger info: ${passenger.name}, ${passenger.info}")
    })
    .recover { case e => e.printStackTrace() }

  Await.ready(runF, Duration.Inf)
}
