import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethod, HttpMethods, HttpRequest, RequestEntity}
import akka.http.scaladsl.unmarshalling.Unmarshal

import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import spray.json._
import DefaultJsonProtocol._
import akka.stream.ActorMaterializer
import scala.concurrent.duration.Duration

object HttpClientSingleRequest extends App {

  implicit val as: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = as.dispatcher

  case class Response(status: Int, body: String)

  implicit val quoteFormat = jsonFormat2(Quote)

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

  val runF = request(HttpMethods.GET, path = "/random")
    .exec
    .map(qu => println(qu.body))
//    .map(response => response.body.parseJson.convertTo[Quote])
//    .map(quote => println(s"Received quote: ${quote.content} by ${quote.author}"))
    .recover{ case e => e.printStackTrace() } // opat na konci pouzivame recover aby sme zistili ci nieco nezlyhalo, tym ze som vsetky future naskladal cez mapy do "jednej" tak staci tento jeden recover

  val runFun = request(HttpMethods.POST, path = "/hello")

  Await.ready(runF, Duration.Inf)
  /*runF.foreach { quote =>
    println(s"Received quote: ${quote.quoteText} by ${quote.author}")
  }*/
}
