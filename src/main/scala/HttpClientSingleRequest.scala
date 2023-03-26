import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethod, HttpMethods, HttpRequest, RequestEntity}
import akka.http.scaladsl.unmarshalling.Unmarshal

import scala.concurrent.{ExecutionContextExecutor, Future}
import spray.json._
import DefaultJsonProtocol._
import akka.stream.ActorMaterializer

object HttpClientSingleRequest extends App {

  implicit val as: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = as.dispatcher

  case class Response(status: Int, body: String)

  implicit val quoteFormat = jsonFormat2(Quote)

  def request(method: HttpMethod, path: String = "", params: Option[String] = None, body: Option[RequestEntity] = None): HttpRequest =
    HttpRequest(
      method = method,
      uri = s"http://localhost:900$path${params.map(p => s"?$p").getOrElse("")}",
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

  val runF = for {
    _ <- request(HttpMethods.GET).exec.map(println)
    _ <- request(HttpMethods.GET, path = "/hello").exec.map(println)
    _ <- request(HttpMethods.GET, path = "/hello_to", params = Some("nameeee=Zoltan")).exec.map(println)
    _ <- request(HttpMethods.GET, path = "/hello_to", params = Some("name=Zoltan")).exec.map(println)
    response <- request(HttpMethods.GET, path = "/quote").exec
    quote <- Future(response.body.parseJson.convertTo[Quote])
  } yield quote

  runF.foreach { quote =>
    println(s"Received quote: ${quote.quoteText} by ${quote.author}")
  }
}
