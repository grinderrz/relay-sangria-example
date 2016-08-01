package com.example

import akka.actor.{ ActorSystem, Props, ActorRef, Actor, ActorLogging }
import akka.stream.actor.{ ActorPublisher, ActorPublisherMessage }
import akka.stream.{ Materializer, ActorMaterializer }
import akka.stream.scaladsl.Source
import akka.http.scaladsl.Http
import com.typesafe.config.ConfigFactory
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes.{ InternalServerError, OK, BadRequest }
import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import sangria.ast.OperationType
import sangria.parser.QueryParser
import scala.util.{ Success, Failure }
import sangria.execution.{ ErrorWithResolver, QueryAnalysisError, Executor, HandledException, PreparedQuery }
import sangria.marshalling.sprayJson._

object Boot extends App {
  private val config = ConfigFactory.load()
  implicit val system = ActorSystem("Example", config)
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  val route =
    (post & path("graphql")) {
      entity(as[JsValue]) { requestJson =>
        val JsObject(fields) = requestJson
        val JsString(query) = fields("query")
        val operation = fields.get("operationName") collect {
          case JsString(op) => op
        }
        val vars = fields.get("variables") match {
          case Some(obj: JsObject) => obj
          //case Some(JsString(s)) if s.trim.nonEmpty => s.parseJson
          case _ => JsObject.empty
        }
        QueryParser.parse(query) match {
          case Success(queryAst) =>
            complete(
              Executor.execute(
                ChatSchema.schema,
                queryAst,
                new ChatData.MessageRepo,
                variables = vars,
                operationName = operation)
              .map(OK -> _)
              .recover {
                case error: QueryAnalysisError => BadRequest -> error.resolveError
                case error: ErrorWithResolver => InternalServerError -> error.resolveError
              })
          case Failure(error) =>
            complete(BadRequest, JsObject("error" -> JsString(error.getMessage)))
        }
      }
    }

  val port = config.getInt("boot.port")
  val interface = config.getString("boot.interface")

  val bindingFuture = Http().bindAndHandle(
    route,
    interface,
    port)
  while(scala.io.StdIn.readLine() != null)
  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
}
