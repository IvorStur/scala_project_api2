import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.concurrent.duration._
import scala.io.StdIn
import slick.jdbc.SQLiteProfile.api._
import scala.concurrent.ExecutionContext

case class Passenger(name: String, info: String)

class Passengers(tag: Tag) extends Table[Passenger](tag, "passengers") {
  def name = column[String]("name", O.PrimaryKey)
  def info = column[String]("info")

  def * = (name, info) <> (Passenger.tupled, Passenger.unapply)
}

object Main extends App {
  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val db = Database.forConfig("slick.db")
  val passengers = TableQuery[Passengers]

  val host = "localhost"
  val port = 8080

  implicit val passengerFormat = jsonFormat2(Passenger)

  def fetchPassengerInfo(name: String): Future[Option[Passenger]] = {
    val query = passengers.filter(_.name === name).result.headOption
    db.run(query)
  }

  def savePassengerInfo(passenger: Passenger): Future[Int] = {
    val insertAction = passengers += passenger
    db.run(insertAction)
  }

  def fetchPassengerInfoFromAPI(): Future[Passenger] = {
    val request = HttpRequest(
      method = HttpMethods.GET,
      uri = "https://api.instantwebtools.net/v1/passenger"
    )

    for {
      response <- Http().singleRequest(request)
      entity <- response.entity.toStrict(100.millis)
      jsonAst = entity.data.utf8String.parseJson
    } yield {
      val passenger = jsonAst.convertTo[Passenger]
      passenger
    }
  }

  def route(implicit ec: ExecutionContext) =
    path("passenger" / Segment) { name =>
      get {
        val passengerInfoF = fetchPassengerInfo(name)

        onSuccess(passengerInfoF.flatMap {
          case Some(passenger) => Future.successful(passenger)
          case None =>
            fetchPassengerInfoFromAPI().flatMap { passenger =>
              savePassengerInfo(passenger).map(_ => passenger)
            }
        }) { passenger =>
          complete(s"${passenger.name}: ${passenger.info}")
        }
      }
    }

  val binding = Http().newServerAt(host, port).bind(route(executionContext))

  println(s"Server online at http://$host:$port/\nPress RETURN to stop...")
  /*StdIn.readLine() // let it run until user presses return
  binding
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => {
      db.close() // Close the database connection
      system.terminate() // and shutdown the actor system
    })*/
}
