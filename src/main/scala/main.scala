import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.io.StdIn

case class Quote(content: String, author: String)

object Main extends App {
//  https://dummyjson.com/users
  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val host = "localhost"
  val port = 8080

  implicit val quoteFormat = jsonFormat2(Quote)

  def getUser() = HttpRequest(
    method = HttpMethods.POST,
    uri = "https://dummyjson.com/users",
    entity = HttpEntity(
      ContentTypes.`text/plain(UTF-8)`, "data"
    )
  )
  def getQuote(): Future[Quote] = {
    val request = HttpRequest(
      method = HttpMethods.GET,
      uri = "https://api.quotable.io/random"
    )

    for {
      response <- Http().singleRequest(request)
      entity <- response.entity.toStrict(100.millis)
      jsonAst = entity.data.utf8String.parseJson
    } yield {
      val quote = jsonAst.convertTo[Quote]
      quote
    }
  }.recover { case e => e.printStackTrace(); throw e } // v recover sa da vyprintovat chyba, tym ze chceme vratit Quote a nie Unit, musime dat nakoniec rethrow tej exception

  def route = path("hello") {
    get {
      complete()
    }
  } ~ path("random") {
    get {
      val quoteF = getQuote()
      onSuccess(quoteF) { quote =>
        complete(s"${quote.author}: ${quote.content}")
//        complete(s"Here's a quote by ${quote.author}: ${quote.content}")
      }
    }
  }
//  Http().bindAndHandle()
val binding = Http().newServerAt("127.0.0.1", 8080).bind(route)

//  println(s"Server online at http://localhost:8081/\nPress RETURN to stop...")
//  StdIn.readLine() // let it run until user presses return
//  binding
//    .flatMap(_.unbind()) // trigger unbinding from the port
//    .onComplete(_ => system.terminate()) // and shutdown when done
}
