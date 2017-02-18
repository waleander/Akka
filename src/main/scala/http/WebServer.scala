package http

/**
  * Created by pabloperezgarcia on 06/02/2017.
  */

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

import scala.concurrent.Future
import scala.io.StdIn
import scala.util.Try

object WebServer extends App {

  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()

  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher

  val localhost = "localhost"
  val port = 8080

  initializeService

  /**
    * Akka http provide the bindAndHandler which allow you to bind using a dsl some endpoints in a specific address/port
    *
    */
  private def initializeService = {
    val serverFuture = Http().bindAndHandle(initializeRoutes, localhost, port)
    serverFuture.onComplete(_ => println(s"Server online at http://$localhost:$port/\nPress RETURN to stop..."))
    StdIn.readLine() // let it run until user presses return
    shutdownService(serverFuture)
  }

  /**
    * Trigger unbinding from the port and shutdown when done
    *
    * @param bindingFuture
    */
  private def shutdownService(bindingFuture: Future[ServerBinding]) = {
    bindingFuture
      .flatMap(_.unbind()) //
      .onComplete(_ ⇒ system.terminate()) //
  }

  /**
    * Akka http provide a dsl to create the endpoints for the rest service
    *
    * @return
    */
  private def initializeRoutes = {
    path("hello") {
      get {

        complete(getHelloResponse)
      }
    }
  }

  def saveOrder(order: String) = Future.apply(order)

  val route: Route =
    get {
      path("order") {
        complete(getHelloResponse)
      }
    } ~
      post {
        path("create-order") {
          entity(as[String]) { order =>
            val saved: Future[String] = saveOrder(order)
            onComplete(saved) { done =>
              complete(getPostResponse(done))
            }
          }
        }
      }

  private def getHelloResponse = {
    HttpEntity(ContentTypes.`text/html(UTF-8)`, "Say hello to akka-http")
  }

  private def getPostResponse(order: Try[String]) = {
    HttpEntity(ContentTypes.`text/html(UTF-8)`, s"Order stored ${order.get} ")
  }
}