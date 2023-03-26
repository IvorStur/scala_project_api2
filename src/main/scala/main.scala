import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.concurrent.duration._

case class Quote(quoteText: String, author: String)

object Main extends App {
  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val host = "0.0.0.0"
  val port = 900

  implicit val quoteFormat = jsonFormat2(Quote)

  def getQuote(): Future[Quote] = {
    val request = HttpRequest(
      method = HttpMethods.GET,
      uri = "https://api.quotable.io/random"
    )

    for {
      response <- Http().singleRequest(request)
      entity <- response.entity.toStrict(100.millis)
      jsonAst <- Future(entity.data.utf8String.parseJson)
    } yield {
      val quote = jsonAst.convertTo[Quote]
      quote
    }
  }

  def route = path("hello") {
    get {
      complete("Hello, World!")
    }
  } ~ path("quote") {
    get {
      val quoteF = getQuote()
      onSuccess(quoteF) { quote =>
        complete(s"Here's a quote by ${quote.author}: ${quote.quoteText}")
      }
    }
  }

  Http().bindAndHandle(route, host, port)
}
